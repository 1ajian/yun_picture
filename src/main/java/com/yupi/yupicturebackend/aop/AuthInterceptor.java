package com.yupi.yupicturebackend.aop;

import com.github.xiaoymin.knife4j.annotations.ApiSort;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.UserRoleEnum;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * ClassName: AuthInterceptor
 * Package: com.yupi.yupicturebackend.aop
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/24 12:51
 * @Version 1.0
 */
@Aspect
@Component
@Slf4j
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     * @param joinPoint 切入点
     * @param authCheck 自定义注解
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //获取注解的属性的值
        String mustRole = authCheck.mustRole();

        //获取请求属性
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        ThrowUtils.throwIf(requestAttributes == null, new BusinessException(ErrorCode.OPERATION_ERROR,"无法获取请求属性信息"));
        //获取请求对象
        HttpServletRequest request = requestAttributes.getRequest();

        //获取Session中存储的对象信息
        User user = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(user == null, new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
        //无需身份验证
        if (mustRole == null) {
            joinPoint.proceed();
        }

        //以下进行身份验证
        String userRole = user.getUserRole();
        //根据用户身份获取身份枚举类型
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userRole);

        //需要身份，但是用户没有
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //需要管理员身份,但不是
        if (UserConstant.ADMIN_ROLE.equals(mustRole) && !UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        return joinPoint.proceed();
    }
}
