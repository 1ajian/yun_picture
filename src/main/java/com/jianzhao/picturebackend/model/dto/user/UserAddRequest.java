package com.jianzhao.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: UserAddRequest
 * Package: com.yupi.yupicturebackend.model.dto.user
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/24 13:36
 * @Version 1.0
 */
@Data
public class UserAddRequest implements Serializable {

    private static final long serialVersionUID = 5802219420603578535L;
    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin,VIP
     */
    private String userRole;

}
