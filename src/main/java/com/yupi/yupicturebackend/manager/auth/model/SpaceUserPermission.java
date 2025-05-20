package com.yupi.yupicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: SpaceUserPermission
 * Package: com.yupi.yupicturebackend.manager.auth.model
 * Description:
 *  空间用户权限
 * @Author 阿小健
 * @Create 2025/5/17 14:15
 * @Version 1.0
 */
@Data
public class SpaceUserPermission implements Serializable {
    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
