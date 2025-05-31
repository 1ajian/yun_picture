package com.jianzhao.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserUpdatePassword
 * Package: com.jianzhao.picturebackend.model.dto.user
 * Description:
 *      更新用户密码
 * @Author 阿小健
 * @Create 2025/5/25 20:53
 * @Version 1.0
 */
@Data
public class UserUpdatePassword implements Serializable {

    private static final long serialVersionUID = 7602972394099330529L;
    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String confirmPassword;
}
