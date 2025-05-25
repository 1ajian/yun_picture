package com.jianzhao.picturebackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ClassName: CapturePictureConfig
 * Package: com.jianzhao.picturebackend.config
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/24 13:00
 * @Version 1.0
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "user-agents")
public class CapturePictureConfig {
    /**
     * 用户代理列表
     */
    private List<String> list;
}
