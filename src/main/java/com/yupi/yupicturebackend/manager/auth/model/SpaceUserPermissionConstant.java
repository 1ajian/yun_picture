package com.yupi.yupicturebackend.manager.auth.model;

/**
 * ClassName: SpaceUserPermissionConstant
 * Package: com.yupi.yupicturebackend.manager.auth.model
 * Description:
 *  空间成员权限常量类
 * @Author 阿小健
 * @Create 2025/5/17 14:17
 * @Version 1.0
 */
public interface SpaceUserPermissionConstant {
    /**
     * 空间用户管理权限
     */
    String SPACE_USER_MANAGE = "spaceUser:manage";

    /**
     * 图片查看权限
     */
    String PICTURE_VIEW = "picture:view";

    /**
     * 图片上传权限
     */
    String PICTURE_UPLOAD = "picture:upload";

    /**
     * 图片编辑权限
     */
    String PICTURE_EDIT = "picture:edit";

    /**
     * 图片删除权限
     */
    String PICTURE_DELETE = "picture:delete";
}

