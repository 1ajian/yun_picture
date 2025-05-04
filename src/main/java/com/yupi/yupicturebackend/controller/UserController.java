package com.yupi.yupicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.user.*;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.LoginUserVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = "用户接口")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    @ApiOperation(value = "用户注册")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, new BusinessException(ErrorCode.PARAMS_ERROR));

        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String userAccount = userRegisterRequest.getUserAccount();
        long res = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(res);
    }

    @PostMapping("/login")
    @ApiOperation(value = "用户登录")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, new BusinessException(ErrorCode.PARAMS_ERROR));
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);

    }

    @GetMapping("/get/login")
    @ApiOperation(value = "获取当前登录用户信息")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        //获取登录用户信息
        User loginUser = userService.getLoginUser(request);
        //获取已登录脱敏的用户信息
        LoginUserVO loginUserVo = userService.getLoginUserVo(loginUser);
        return ResultUtils.success(loginUserVo);
    }

    @PostMapping("/logout")
    @ApiOperation(value = "用户注销")
    public BaseResponse<Boolean> userLoginout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @PostMapping("/add")
    @ApiOperation(value = "创建用户")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
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


    @ApiOperation(value = "根据 id 获取用户（仅管理员）")
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {
        ThrowUtils.throwIf(id <= 0, new BusinessException(ErrorCode.PARAMS_ERROR));
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        return ResultUtils.success(user);
    }


    @ApiOperation(value = "根据 id 获取包装类UserVO")
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {
        BaseResponse<User> userBaseResponse = getUserById(id);
        User user = userBaseResponse.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    @ApiOperation(value = "删除用户信息")
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf((deleteRequest == null || deleteRequest.getId() <= 0), new BusinessException(ErrorCode.PARAMS_ERROR));
        boolean res = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(res);
    }

    @ApiOperation(value = "更新用户信息")
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

    @ApiOperation(value = "分页获取用户封装列表（仅管理员）")
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


}
