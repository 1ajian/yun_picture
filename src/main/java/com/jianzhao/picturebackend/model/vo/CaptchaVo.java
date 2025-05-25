package com.jianzhao.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: CaptchaVo
 * Package: com.jianzhao.picturebackend.model.vo
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/22 21:23
 * @Version 1.0
 */
@Data
public class CaptchaVo implements Serializable {

    private static final long serialVersionUID = -1934259828354896740L;

    /**
     * 验证码图片信息
     */
    private String image;

    /**
     * 验证码key（缓存key）
     */
    private String key;
}
