package com.yupi.yupicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: SpaceEditRequest
 * Package: com.yupi.yupicturebackend.model.dto.space
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/9 15:05
 * @Version 1.0
 */
@Data
public class SpaceEditRequest implements Serializable {
    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    private static final long serialVersionUID = 1L;
}
