package com.jianzhao.picturebackend.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
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
import net.bytebuddy.implementation.bytecode.Throw;
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

    /**
     * 用户注册
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
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        String encryptPassword = userService.getEncryptPassword(UserConstant.DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        if (StrUtil.isBlank(userAddRequest.getUserName())) {
            user.setUserName("图友" + UUID.randomUUID().toString().replaceAll("-", "").substring(0,8));
        }

        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, new BusinessException(ErrorCode.OPERATION_ERROR));
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


}
