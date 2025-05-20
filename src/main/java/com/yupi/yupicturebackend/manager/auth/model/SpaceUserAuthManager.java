package com.yupi.yupicturebackend.manager.auth.model;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.SpaceUser;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.SpaceRoleEnum;
import com.yupi.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yupi.yupicturebackend.service.SpaceUserService;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ClassName: SpaceUserAuthManager
 * Package: com.yupi.yupicturebackend.manager.auth.model
 * Description:
 *  空间用户认证管理
 * @Author 阿小健
 * @Create 2025/5/17 14:20
 * @Version 1.0
 */
@Component
@SuppressWarnings("all")
public class SpaceUserAuthManager {
    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    //加载SpaceUserAuthConfig(读取配置文件.json)
    public final static SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);

    }

    /**
     * 根据角色获取权限列表
     * @param spaceUserRole
     * @return
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        //判空
        if (StrUtil.isBlank(spaceUserRole)) {
            return Collections.emptyList();
        }

        //过滤和指定空间用户角色key相同的空间角色
        SpaceUserRole suRole = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(role -> role.getKey().equals(spaceUserRole))
                .findFirst()
                .orElse(null);

        if (suRole == null) {
            return new ArrayList<>();
        }

        return suRole.getPermissions();
    }

    /**
     * 返回权限列表
     * @param
     * @return
     */
    public List<String> getPermissionsByRole(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }

        //管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        //公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }

            return new ArrayList<>();
        }

        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());

        if (spaceTypeEnum == null) {
            return Collections.emptyList();
        }

        //空间类型
        switch (spaceTypeEnum) {
            case PRIVATE:
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();

                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }

        return new ArrayList<>();
    }
}
