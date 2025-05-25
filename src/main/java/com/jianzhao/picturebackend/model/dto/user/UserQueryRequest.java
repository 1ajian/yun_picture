package com.jianzhao.picturebackend.model.dto.user;

import com.jianzhao.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * ClassName: UserQueryRequest
 * Package: com.yupi.yupicturebackend.model.dto.user
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/24 13:39
 * @Version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = -737444026668933146L;

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;
}
