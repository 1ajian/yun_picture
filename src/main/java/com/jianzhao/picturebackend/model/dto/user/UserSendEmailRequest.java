package com.jianzhao.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserSendEmailRequest
 * Package: com.jianzhao.picturebackend.model.dto.user
 * Description:
 *  用户发送邮箱请求
 * @Author 阿小健
 * @Create 2025/5/25 14:19
 * @Version 1.0
 */
@Data
public class UserSendEmailRequest implements Serializable {

    private static final long serialVersionUID = 2551881822504719037L;

    /**
     * 用户邮箱
     */
    private String userEmail;

    /**
     * 密码
     */
    private String password;

    /**
     * 确定密码
     */
    private String checkPassword;

    /**
     * 缓存key
     */
    private String key;

    /**
     * 验证码
     */
    private String code;

    /**
     * 邀请码
     */
    private String otherShareCode;

}
