package com.jianzhao.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ClassName: UpdateUserInfoRequest
 * Package: com.jianzhao.picturebackend.model.dto.user
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/25 20:46
 * @Version 1.0
 */
@Data
public class UpdateUserInfoRequest implements Serializable {

    private static final long serialVersionUID = -3917107362367517832L;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     *  用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 手机号
     */
    private String phone;
}
