package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: PictureUploadRequest
 * Package: com.yupi.yupicturebackend.model.dto.picture
 * Description:
 *      文件上传请求类
 * @Author 阿小健
 * @Create 2025/4/25 15:09
 * @Version 1.0
 */
@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = 2243373394424194977L;

    /**
     * 图片id(用于修改)
     */
    private Long id;
}
