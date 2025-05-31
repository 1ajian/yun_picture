package com.jianzhao.picturebackend.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzhao.picturebackend.constant.UserConstant;
import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import com.jianzhao.picturebackend.exception.ThrowUtils;
import com.jianzhao.picturebackend.manager.auth.StpKit;
import com.jianzhao.picturebackend.mapper.UserMapper;
import com.jianzhao.picturebackend.model.dto.user.UserQueryRequest;
import com.jianzhao.picturebackend.model.dto.user.VipCode;
import com.jianzhao.picturebackend.model.entity.User;
import com.jianzhao.picturebackend.model.vo.CaptchaVo;
import com.jianzhao.picturebackend.model.vo.LoginUserVO;
import com.jianzhao.picturebackend.model.vo.UserVO;
import com.jianzhao.picturebackend.service.UserService;
import com.jianzhao.picturebackend.utils.EmailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
* @author 86135
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-04-23 20:03:34
*/
@Service
@Slf4j
@SuppressWarnings("all")
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {
    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${nickname}")
    private String nickname;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${subject-prefix}")
    private String subjectPrefix;

    private final ReentrantLock fileLock = new ReentrantLock();

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param otherShareCode 分享码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String otherShareCode) {
        //1.校验
        if (StrUtil.hasBlank(userAccount,userPassword,checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }

        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }

        ThrowUtils.throwIf(userPassword.length() < 8 || checkPassword.length() < 8,new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短"));

        ThrowUtils.throwIf(!userPassword.equals(checkPassword),new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入的密码不一致"));

        //2.查询是否重复
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>().eq(User::getUserAccount, userAccount);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, new BusinessException(ErrorCode.PARAMS_ERROR,"账号重复"));

        //3.加密
        String encryptPassword = getEncryptPassword(userPassword);
        
        //4.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("图友"+ UUID.randomUUID().toString().replaceAll("-", "").substring(0,8));
        String shareCode = UUID.randomUUID().toString().replace("-", "");
        //设置分享码
        user.setShareCode(shareCode);
        //设置邀请用户的Id
        if (StrUtil.isNotBlank(otherShareCode)) {
            User shareUser = this.query().eq("id", otherShareCode).one();
            if (shareUser != null) {
                user.setInviteUser(shareUser.getId());
            }

        }

        boolean saveRes = this.save(user);

        ThrowUtils.throwIf(!saveRes, new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败,数据库错误"));

        //发送邮件通知注册成功
        return user.getId();
    }

    /**
     * 获取图片验证码
     * @return
     */
    @Override
    public CaptchaVo getCaptcha() {

        CircleCaptcha circleCaptcha = CaptchaUtil.createCircleCaptcha(130, 48, 4, 5);
        //验证码
        String code = circleCaptcha.getCode();
        code = code.toLowerCase();
        //填入验证码
        circleCaptcha.createImage(code);
        //获取Base64编码字符串
        String imageBase64Data = circleCaptcha.getImageBase64Data();

        //随机八位缓存后缀
        String keySuffer = RandomUtil.randomString(8);
        String key = UserConstant.CAPTCHA_CODE_KEY + keySuffer;
        //验证码存入缓存
        stringRedisTemplate.opsForValue().set(key, code);
        //设置五分钟过期
        stringRedisTemplate.expire(key,5, TimeUnit.MINUTES);
        //构建返回对象
        CaptchaVo captchaVo = new CaptchaVo();
        captchaVo.setImage(imageBase64Data);
        captchaVo.setKey(key);

        return captchaVo;
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param captchaKey
     * @param captchaCode
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, String captchaKey, String captchaCode, HttpServletRequest request) {
        //校验
        if (StrUtil.hasBlank(userAccount,userPassword,captchaCode,captchaKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }

        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不符合要求");
        }

        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不符合要求");
        }

        if (captchaCode.length() != 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"验证码有误");
        }

        //redis获取验证码
        String lowerCode = captchaCode.toLowerCase();
        String code = stringRedisTemplate.opsForValue().get(captchaKey);
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不存在或已过期!");
        }
        if (!lowerCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码有误");
        }

        //加密
        String encryptPassword = getEncryptPassword(userPassword);

        //验证账号和密码是否正确
        LambdaQueryWrapper<User> queryWrapper = null;
        //先要判断是使用什么方式登录的
        if (ReUtil.isMatch(RegexPool.EMAIL,userAccount)) {
            //使用邮箱登录
            queryWrapper = new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, userAccount)
                    .eq(User::getUserPassword,encryptPassword);
        }else {
            //使用账号登录
            queryWrapper = new LambdaQueryWrapper<User>()
                    .eq(User::getUserAccount, userAccount)
                    .eq(User::getUserPassword, encryptPassword);
        }

        User user = this.baseMapper.selectOne(queryWrapper);

        //不正确
        if (null == user) {
            log.info("user login failed,userAccount can't match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或者密码错误");
        }

        //将用户信息放入Session
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

        //用户登录成功后,保存登录状态到Sa-Token的空间账号体系中

        /*
        * 生成一个 Token（类似登录凭证）。
        * 把用户 ID 和 Token 关联起来（存到 Redis 或内存）。
        * 后续可以通过 Token 识别用户身份。
        * */
        //记录用户登录态到Sa-token,便于空间鉴权时使用,注意保证该用户与SpringSession中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        //删除缓存的验证码记录
        stringRedisTemplate.delete(captchaKey);

        return this.getLoginUserVo(user);
    }

    /**
     * 获取脱敏已登录用户信息
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVo(User user) {
        if (user == null) {
            return null;
        }

        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);

        return loginUserVO;
    }

    /**
     * 获取加密密码
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "axiaojian";
        
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 获取当前登录用户信息
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        //为了保证拿到的数据是最新,再去数据库查
        Long userId = user.getId();
        user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        //先知道用户是否已登录
        User user = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(user == null,new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"未登录"));
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取单个用户脱敏信息
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏用户信息列表
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (userList.isEmpty()) {
            return Collections.emptyList();
        }

        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 查询请求转为 QueryWrapper 对象
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数对象为空"));

        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userRole = userQueryRequest.getUserRole();
        String userProfile = userQueryRequest.getUserProfile();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        Date beginVipExpireTime = userQueryRequest.getBeginVipExpireTime();
        Date endVipExpireTime = userQueryRequest.getEndVipExpireTime();
        String email = userQueryRequest.getEmail();
        String phone = userQueryRequest.getPhone();
        return new QueryWrapper<User>()
                .eq(ObjUtil.isNotNull(id), "id", id)
                .like(ObjUtil.isNotEmpty(userName), "userName", userName)
                .like(ObjUtil.isNotEmpty(userAccount), "userAccount", userAccount)
                .eq(ObjUtil.isNotEmpty(userRole), "userRole", userRole)
                .like(ObjUtil.isNotEmpty(userProfile), "userProfile", userProfile)
                .like(ObjUtil.isNotEmpty(email), "email", email)
                .like(ObjUtil.isNotEmpty(phone), "phone", phone)
                .ge(ObjUtil.isNotNull(beginVipExpireTime), "vipExpireTime", beginVipExpireTime)
                .lt(ObjUtil.isNotNull(endVipExpireTime), "vipExpireTime", endVipExpireTime)
                .orderBy(StrUtil.isNotBlank(sortField),"ascend".equals(sortOrder),sortField);
    }

    /**
     * 判断用户是否为管理员
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && (user.getUserRole().equals(UserConstant.ADMIN_ROLE));
    }


    /**
     * 兑换会员
     * @param user
     * @param vipCode
     * @return
     */
    @Override
    public boolean exchangeVip(User user, String vipCode) {
        //校验参数
        if (user == null || StrUtil.isBlank(vipCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //读取并校验验证码
        VipCode targetCode = validateAndMarkVipCode(vipCode);

        //更新用户信息
        updateUserVipInfo(user,targetCode);

        return true;
    }


    /**
     * 读取并校验、使用验证码
     * @param vipCode
     * @return
     */
    private VipCode validateAndMarkVipCode(String vipCode) {
        //获取锁保证一次只有一个线程可以操作
        fileLock.lock();

        try {
            JSONArray jsonArray = readVipCodeFile();

            //将jsonArray转List
            List<VipCode> vipCodeList = JSONUtil.toList(jsonArray, VipCode.class);

            VipCode codes = vipCodeList.stream()
                    .filter(code -> vipCode.equals(code.getVipCode()) && !code.isHasUsed() && ObjUtil.isNotNull(code.getTime()) )
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "无效的兑换码"));

            codes.setHasUsed(true);
            // 写回文件
            writeVipCodeFile(JSONUtil.parseArray(vipCodeList));
            return codes;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * 写入文件
     * @param jsonArray
     */
    private void writeVipCodeFile(JSONArray jsonArray) {
        try {
            Resource resource = resourceLoader.getResource("classpath:biz/vipCode.json");
            FileUtil.writeString(jsonArray.toStringPretty(), resource.getFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作有误");
        }

    }

    /**
     * 读取文件
     * @return
     */
    private JSONArray readVipCodeFile() {
        String json = ResourceUtil.readUtf8Str("biz/vipCode.json");
        JSONArray jsonArray = JSONUtil.parseArray(json);
        return jsonArray;
    }

    /**
     * 更新用户vip
     * @param user
     * @param targetCode
     */
    private void updateUserVipInfo(User user, VipCode targetCode) {
        //获取过期时间
        Date vipExpireTime = user.getVipExpireTime();
        //如果为空  先给初始值
        if (vipExpireTime == null) {
            vipExpireTime = new Date();
        }
        //创建要更新的对象
        User updateUser = new User();
        Integer time = targetCode.getTime();

        //根据兑换码的时间 设置对象会员过期时间
        switch (time) {
            case 1:
                updateUser.setVipExpireTime(DateUtil.offsetDay(vipExpireTime,30));
                break;
            case 6:
                updateUser.setVipExpireTime(DateUtil.offsetMonth(vipExpireTime, 6));
                break;

            case 12:
                updateUser.setVipExpireTime(DateUtil.offsetDay(vipExpireTime,365));
                break;
        }

        //如果当前用户是普通用户才给他设置为vip
        if (user.getUserRole().equals(UserConstant.DEFAULT_ROLE)) {
            updateUser.setUserRole(UserConstant.VIP_ROLE);
        }

        updateUser.setVipCode(targetCode.getVipCode());
        updateUser.setId(user.getId());
        //updateUser.setShareCode("VIP_" + targetCode.getVipCode());
        updateUser.setVipNumber(user.getId());
        //updateUser.setVipExpireTime(DateUtil.offsetDay(vipExpireTime == null ? new Date() : vipExpireTime,365));

        boolean success = this.updateById(updateUser);

        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作数据库错误");
        }
    }

    /**
     * 发送邮件验证码
     * @param userEmail
     * @return
     */
    @Override
    public String sendEmailCode(String userEmail) {
        //首先需要先校验当前邮箱是否被使用
        boolean exists = this.lambdaQuery().eq(User::getEmail, userEmail).exists();
        if (exists) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已被使用,请直接登录!");
        }

        //先构建缓存key
        String key = UUID.randomUUID().toString().replace("-", "").substring(0,8);
        String cacheKey = String.format(UserConstant.EMAIL_CODE_KEY, userEmail,key);
        //构建验证码
        String code = RandomUtil.randomNumbers(4);
        stringRedisTemplate.opsForValue().set(cacheKey, code, 300, TimeUnit.SECONDS);
        //发送验证码
        sendEmailAsCode(userEmail, "许小健智能AI图库 - 注册验证码", code);
        return key;
    }

    /**
     * 邮箱注册
     * @param userEmail
     * @param password
     * @param checkPassword
     * @param key
     * @param code
     * @param otherShareCode
     * @return
     */
    @Override
    public Long registerEmail(String userEmail, String password, String checkPassword, String key, String code, String otherShareCode) {
        if (StrUtil.hasBlank(userEmail, password, checkPassword, key, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //再次校验当前用户是否存在
        boolean exists = this.lambdaQuery().eq(User::getEmail, userEmail).exists();
        ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "该邮箱已被使用!");
        //校验邮箱
        if (!ReUtil.isMatch(RegexPool.EMAIL, userEmail)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式有误!");
        }

        //校验验证码是否一致
        String cacheKey = String.format(UserConstant.EMAIL_CODE_KEY,userEmail,key);
        String cacheCode = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cacheCode == null || !cacheCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误!");
        }
        //删除缓存验证码
        stringRedisTemplate.delete(cacheKey);

        //校验密码
        ThrowUtils.throwIf(password.length() < 8 || checkPassword.length() < 8,new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短"));
        if (StrUtil.isBlank(password) || StrUtil.isBlank(checkPassword) || !password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致!");
        }
        //加入数据库
        User user = new User();
        //密码加密
        String encryptPassword = this.getEncryptPassword(password);
        user.setUserPassword(encryptPassword);
        user.setUserAccount(userEmail);
        user.setEmail(userEmail);
        user.setUserName("图友-" + UUID.randomUUID().toString().replace("-", "").substring(0,8));
        String shareCode = UUID.randomUUID().toString().replace("-", "");
        user.setShareCode(shareCode);

        if (StrUtil.isNotBlank(otherShareCode)) {
            User shareUser = this.query().eq("shareCode", otherShareCode).one();
            if (shareUser != null) {
                user.setInviteUser(shareUser.getId());
            }
        }
        boolean success = this.save(user);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "注册失败");
        sendEmailAsRegisterSuccess(userEmail,subjectPrefix + "注册成功通知",userEmail,password);
        return user.getId();
    }

    /**
     *  发送邮箱验证码
     * @param userEmail
     * @param subject
     * @param code
     */
    @Async("threadPoolExecutor")
    private void sendEmailAsCode(String userEmail, String subject,String code) {
        HashMap<String, Object> param = new HashMap<>();
        param.put("code", code);
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            //组合邮箱发送内容
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setFrom(nickname + "<" + from + ">");
            messageHelper.setTo(userEmail);
            messageHelper.setSubject(subject);
            messageHelper.setText(EmailUtils.emailContentTemplate("templates/EmailCodeTemplate.html", param),true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发送邮件失败");
        }
    }

    /**
     * 发送注册成功邮件
     * @param userEmail
     * @param subject
     * @param userAccount
     * @param password
     */
    @Override
    @Async("threadPoolExecutor")
    public void sendEmailAsRegisterSuccess(String userEmail, String subject, String userAccount, String password) {
        HashMap<String, Object> param = new HashMap<>();
        param.put("account", userAccount);
        param.put("password", password);
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setFrom(nickname + "<" + from + ">");
            messageHelper.setTo(userEmail);
            messageHelper.setSubject(subject);
            messageHelper.setText(EmailUtils.emailContentTemplate("templates/EmailRegisterSuccessTemplate.html", param), true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}




