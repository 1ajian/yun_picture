package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.mapper.UserMapper;
import com.yupi.yupicturebackend.model.dto.user.UserQueryRequest;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.LoginUserVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.yupi.yupicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author 86135
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-04-23 20:03:34
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {


    /**
     * 用户注册
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
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
        boolean saveRes = this.save(user);

        ThrowUtils.throwIf(!saveRes, new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败,数据库错误"));
        return user.getId();
    }

    /**
     * 用户登录
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //校验
        if (StrUtil.hasBlank(userAccount,userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }

        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不符合要求");
        }

        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不符合要求");
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
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
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
        User user = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
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
        User user = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        ThrowUtils.throwIf(user == null,new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"未登录"));
        request.getSession().removeAttribute(USER_LOGIN_STATE);
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


}




