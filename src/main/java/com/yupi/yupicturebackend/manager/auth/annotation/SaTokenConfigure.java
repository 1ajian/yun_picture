package com.yupi.yupicturebackend.manager.auth.annotation;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;

/**
 * ClassName: SaTokenConfigure
 * Package: com.yupi.yupicturebackend.manager.auth.annotation
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/18 11:13
 * @Version 1.0
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    // 注册Sa-Token拦截器,打开注解鉴权功能
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册Sa-Token拦截器 打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }

    @PostConstruct
    public void rewriteSaStrategy() {
        //重写Sa-Token的注解拦截器，增加注解合并功能
        SaAnnotationStrategy.instance.getAnnotation = (element,annotationClass) -> {
            return AnnotatedElementUtils.getMergedAnnotation(element,annotationClass);
        };
    }
}
