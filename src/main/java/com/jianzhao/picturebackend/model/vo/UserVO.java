package com.jianzhao.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ClassName: UserVO
 * Package: com.yupi.yupicturebackend.model.vo
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/24 13:41
 * @Version 1.0
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = -953932945072914626L;
    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 会员过期时间
     */
    private Date vipExpireTime;

    /**
     * 会员兑换码
     */
    private String vipCode;

    /**
     * 会员编号
     */
    private Long vipNumber;

}
