package com.jianzhao.picturebackend.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
    public LoginUserVO userLogin(String userAccount, String userPassword,String captchaKey,String captchaCode, HttpServletRequest request) {
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
        if (!lowerCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码有误");
        }

        //加密
        String encryptPassword = getEncryptPassword(userPassword);

        //验证账号和密码是否正确
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUserAccount, userAccount)
                .eq(User::getUserPassword, encryptPassword);

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
        return new QueryWrapper<User>()
                .eq(ObjUtil.isNotNull(id), "id", id)
                .like(ObjUtil.isNotEmpty(userName), "userName", userName)
                .like(ObjUtil.isNotEmpty(userAccount), "userAccount", userAccount)
                .eq(ObjUtil.isNotEmpty(userRole), "userRole", userRole)
                .like(ObjUtil.isNotEmpty(userProfile), "userProfile", userProfile)
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


}




