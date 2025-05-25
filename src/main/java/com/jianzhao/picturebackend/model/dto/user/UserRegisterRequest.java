package com.jianzhao.picturebackend.model.dto.user;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;

/**
 * ClassName: UserRegisterRequest
 * Package: com.yupi.yupicturebackend.model.user
 * Description:
 *  `   用户注册请求参数
 * @Author 阿小健
 * @Create 2025/4/23 20:26
 * @Version 1.0
 */
@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;


}
