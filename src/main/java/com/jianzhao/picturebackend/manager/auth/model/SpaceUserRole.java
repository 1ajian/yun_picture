package com.jianzhao.picturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ClassName: SpaceUserRole
 * Package: com.yupi.yupicturebackend.manager.auth.model
 * Description:
 *  空间用户角色
 * @Author 阿小健
 * @Create 2025/5/17 14:15
 * @Version 1.0
 */
@Data
public class SpaceUserRole implements Serializable {
    /**
     * 角色键
     */
    private String key;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 权限键列表
     */
    private List<String> permissions;

    /**
     * 角色描述
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
