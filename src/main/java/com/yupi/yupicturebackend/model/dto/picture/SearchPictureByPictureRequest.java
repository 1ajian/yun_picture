package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: SearchPictureByPictureRequest
 * Package: com.yupi.yupicturebackend.model.dto.picture
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/13 11:48
 * @Version 1.0
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {

    private static final long serialVersionUID = 8889658264647949098L;
    /**
     * 图片Id
     */
    private Long pictureId;


}
