package com.yupi.yupicturebackend.annotation;

import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ClassName: AuthCheck
 * Package: com.yupi.yupicturebackend.annotation
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/24 12:49
 * @Version 1.0
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AuthCheck {

    /**
     * 必须有某个角色
     * @return
     */
    String mustRole() default "";
}
