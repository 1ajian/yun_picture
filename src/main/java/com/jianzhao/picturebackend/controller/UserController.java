package com.jianzhao.picturebackend.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzhao.picturebackend.annotation.AuthCheck;
import com.jianzhao.picturebackend.common.BaseResponse;
import com.jianzhao.picturebackend.common.DeleteRequest;
import com.jianzhao.picturebackend.common.ResultUtils;
import com.jianzhao.picturebackend.constant.UserConstant;
import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import com.jianzhao.picturebackend.exception.ThrowUtils;
import com.jianzhao.picturebackend.model.dto.user.*;
import com.jianzhao.picturebackend.model.entity.User;
import com.jianzhao.picturebackend.model.vo.CaptchaVo;
import com.jianzhao.picturebackend.model.vo.LoginUserVO;
import com.jianzhao.picturebackend.model.vo.UserVO;
import com.jianzhao.picturebackend.service.UserService;
import com.jianzhao.picturebackend.utils.EmailUtils;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

/**
 * ClassName: UserController
 * Package: com.yupi.yupicturebackend.controller
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/23 20:50
 * @Version 1.0
 */
@RestController
@RequestMapping("/user")
//@Api(tags = "用户接口")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户注册 (使用账号密码注册)
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    //@ApiOperation(value = "用户注册")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest,String shareCode) {
        ThrowUtils.throwIf(userRegisterRequest == null, new BusinessException(ErrorCode.PARAMS_ERROR));

        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String userAccount = userRegisterRequest.getUserAccount();
        long res = userService.userRegister(userAccount, userPassword, checkPassword,shareCode);
        return ResultUtils.success(res);
    }

    /**
     * 用户注册发送邮箱验证码
     * @param userEmail
     * @return key
     */
    @PostMapping("/register/send/email")
    public BaseResponse<String> sendEmailRegisterCode(String userEmail) {
        ThrowUtils.throwIf(StrUtil.isBlank(userEmail), ErrorCode.PARAMS_ERROR);
        //校验邮箱
        if (!ReUtil.isMatch(RegexPool.EMAIL, userEmail)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式有误");
        }

        //发送邮箱 并返回结果
        return ResultUtils.success(userService.sendEmailCode(userEmail));
    }

    /**
     * 邮箱注册
     * @param userSendEmailRequest
     * @return
     */
    @PostMapping("/register/email")
    public BaseResponse<Long> emailRegister(@RequestBody UserSendEmailRequest userSendEmailRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(userSendEmailRequest), ErrorCode.PARAMS_ERROR);
        String otherShareCode = userSendEmailRequest.getOtherShareCode();
        String userEmail = userSendEmailRequest.getUserEmail();
        String password = userSendEmailRequest.getPassword();
        String checkPassword = userSendEmailRequest.getCheckPassword();
        String key = userSendEmailRequest.getKey();
        String code = userSendEmailRequest.getCode();
        Long userId = userService.registerEmail(userEmail,password,checkPassword,key,code,otherShareCode);
        return ResultUtils.success(userId);
    }

    /**
     * 获取图片验证码
     * @return
     */
    @GetMapping("/login/captcha")
    public BaseResponse<CaptchaVo> getCaptchaCode() {
        CaptchaVo captcha = userService.getCaptcha();
        return ResultUtils.success(captcha);
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    //@ApiOperation(value = "用户登录")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, new BusinessException(ErrorCode.PARAMS_ERROR));
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        String captchaKey = userLoginRequest.getCaptchaKey();
        String captchaCode = userLoginRequest.getCaptchaCode();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword,captchaKey,captchaCode ,request);
        return ResultUtils.success(loginUserVO);
    }


    /**
     * 获取当前登录用户信息
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    //@ApiOperation(value = "获取当前登录用户信息")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        //获取登录用户信息
        User loginUser = userService.getLoginUser(request);
        //获取已登录脱敏的用户信息
        LoginUserVO loginUserVo = userService.getLoginUserVo(loginUser);
        return ResultUtils.success(loginUserVo);
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @PostMapping("/logout")
    //@ApiOperation(value = "用户注销")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    //@ApiOperation(value = "创建用户")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE})
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        String email = userAddRequest.getEmail();
        String account = userAddRequest.getUserAccount();

        if (StrUtil.isBlank(email) && StrUtil.isBlank(account)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或邮箱不能为空");
        }

        String encryptPassword = userService.getEncryptPassword(UserConstant.DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        if (StrUtil.isBlank(userAddRequest.getUserName())) {
            user.setUserName("图友" + UUID.randomUUID().toString().replaceAll("-", "").substring(0,8));
        }

        if (StrUtil.isBlank(account) && StrUtil.isNotBlank(email)) {
            user.setUserAccount(email);
        }

        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, new BusinessException(ErrorCode.OPERATION_ERROR));
        //判断如果请求参数中有邮箱且邮箱不为空则发送邮件验证码
        if (StrUtil.isBlank(account) && StrUtil.isNotBlank(email)) {
            userService.sendEmailAsRegisterSuccess(email, "许小健智能AI图库 - 添加用户成功通知",email,UserConstant.DEFAULT_PASSWORD);
        }
        return ResultUtils.success(user.getId());
    }


    /**
     * 根据 id 获取用户（仅管理员）
     * @param id
     * @return
     */
    //@ApiOperation(value = "根据 id 获取用户（仅管理员）")
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {
        ThrowUtils.throwIf(id <= 0, new BusinessException(ErrorCode.PARAMS_ERROR));
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        return ResultUtils.success(user);
    }


    /**
     * 根据 id 获取包装类UserVO
     * @param id
     * @return
     */
    //@ApiOperation(value = "根据 id 获取包装类UserVO")
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {
        BaseResponse<User> userBaseResponse = getUserById(id);
        User user = userBaseResponse.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户信息
     * @param deleteRequest
     * @return
     */
    //@ApiOperation(value = "删除用户信息")
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf((deleteRequest == null || deleteRequest.getId() <= 0), new BusinessException(ErrorCode.PARAMS_ERROR));
        boolean res = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(res);
    }

    /**
     * 更新用户信息
     * @param userUpdateRequest
     * @return
     */
    //@ApiOperation(value = "更新用户信息")
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);

        Long userId = userUpdateRequest.getId();
        User oldUser = userService.getById(userId);
        ThrowUtils.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR);

        //如果邮箱和账号是一样的，修改邮箱就要顺便改账号
        if (StrUtil.isNotBlank(userUpdateRequest.getEmail()) && oldUser.getEmail().equals(oldUser.getUserAccount())) {
            user.setUserAccount(userUpdateRequest.getEmail());
        }

        boolean res = userService.updateById(user);
        ThrowUtils.throwIf(!res, new BusinessException(ErrorCode.OPERATION_ERROR ));
        return ResultUtils.success(res);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     * @param userQueryRequest
     * @return
     */
    //@ApiOperation(value = "分页获取用户封装列表（仅管理员）")
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, new BusinessException(ErrorCode.PARAMS_ERROR));
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();

        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current,pageSize,userPage.getTotal());
        List<User> userList = userPage.getRecords();
        List<UserVO> userVOList = userService.getUserVOList(userList);
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 兑换vip
     * @param vipExchangeRequest
     * @param request
     * @return
     */
    @PostMapping("/exchange/vip")
    public BaseResponse<Boolean> exchangeVip(@RequestBody VipExchangeRequest vipExchangeRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(vipExchangeRequest == null, ErrorCode.PARAMS_ERROR);

        //获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        boolean result = userService.exchangeVip(loginUser, vipExchangeRequest.getVipCode());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户信息
     * @param updateUserInfoRequest
     * @param request
     * @return
     */
    @PostMapping("/update/userInfo")
    public BaseResponse<Boolean> updateUserVipInfo(@RequestBody UpdateUserInfoRequest updateUserInfoRequest, HttpServletRequest request) {
        ThrowUtils.throwIf( updateUserInfoRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtil.copyProperties(updateUserInfoRequest,user);
        user.setId(loginUser.getId());
        boolean success = userService.updateById(user);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "更新失败");
        return ResultUtils.success(success);
    }

    /**
     * 用户修改密码
     * @param userUpdatePassword
     * @param request
     * @return
     */
    @PostMapping("/update/password")
    public BaseResponse<Boolean> updatePassword(@RequestBody UserUpdatePassword userUpdatePassword,HttpServletRequest request) {

        ThrowUtils.throwIf(userUpdatePassword == null, ErrorCode.PARAMS_ERROR);
        String oldPassword = userUpdatePassword.getOldPassword();
        String newPassword = userUpdatePassword.getNewPassword();
        String confirmPassword = userUpdatePassword.getConfirmPassword();

        if (StrUtil.hasBlank(oldPassword, newPassword, confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        ThrowUtils.throwIf(newPassword.length() < 8 || newPassword.length() > 16, ErrorCode.PARAMS_ERROR, "密码长度不小于8位，不大于16位");

        if (!newPassword.equals(confirmPassword)) {
             throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        }

        //获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        String userPasswordDb = loginUser.getUserPassword();
        String encryptPassword = userService.getEncryptPassword(oldPassword);
        if (!userPasswordDb.equals(encryptPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码错误");
        }

        User user = new User();
        user.setId(loginUser.getId());
        user.setUserPassword(userService.getEncryptPassword(newPassword));
        boolean success = userService.updateById(user);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "修改密码失败");
        return ResultUtils.success(success);
    }

}
