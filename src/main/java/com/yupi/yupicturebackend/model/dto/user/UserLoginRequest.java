package com.yupi.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserLoginRequest
 * Package: com.yupi.yupicturebackend.model.user
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/23 21:08
 * @Version 1.0
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 8336544315841422270L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
