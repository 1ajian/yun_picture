package com.yupi.yupicturebackend;

import com.yupi.yupicturebackend.api.aliyunai.AliYunAiApi;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
//这样就可以随机获取当前的代理对象
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.yupi.yupicturebackend.mapper")
@EnableAsync
public class YuPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuPictureBackendApplication.class, args);

    }

}
