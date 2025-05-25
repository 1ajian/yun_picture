package com.jianzhao.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserUpdateRequest
 * Package: com.yupi.yupicturebackend.model.dto.user
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/24 13:37
 * @Version 1.0
 */
@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = -2836718653125024693L;

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;


}
