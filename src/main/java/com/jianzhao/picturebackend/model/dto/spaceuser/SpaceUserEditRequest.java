package com.jianzhao.picturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: SpaceUserEditRequest
 * Package: com.yupi.yupicturebackend.model.dto.spaceuser
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/16 23:35
 * @Version 1.0
 */
@Data
public class SpaceUserEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}

