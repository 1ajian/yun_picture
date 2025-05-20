package com.yupi.yupicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: SpaceUserAddRequest
 * Package: com.yupi.yupicturebackend.model.dto.spaceuser
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/16 23:34
 * @Version 1.0
 */
@Data
public class SpaceUserAddRequest implements Serializable {

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}

