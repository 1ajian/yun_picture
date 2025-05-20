package com.yupi.yupicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ClassName: SpaceUserAuthConfig
 * Package: com.yupi.yupicturebackend.manager.auth.model
 * Description:
 *  空间用户认证配置类
 * @Author 阿小健
 * @Create 2025/5/17 14:12
 * @Version 1.0
 */
@Data
public class SpaceUserAuthConfig implements Serializable {

    private static final long serialVersionUID = -8694141260470435581L;

    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;

    /**
     * 角色列表
     */
    private List<SpaceUserRole> roles;
}
