package com.jianzhao.picturebackend.model.dto.picture;

import lombok.Data;

/**
 * ClassName: SearchPictureByColorRequest
 * Package: com.yupi.yupicturebackend.model.dto.picture
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/13 14:11
 * @Version 1.0
 */
@Data
public class SearchPictureByColorRequest {
    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
