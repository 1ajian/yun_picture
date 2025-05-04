package com.yupi.yupicturebackend.model.dto.file;

import lombok.Data;

/**
 * ClassName: UploadPictureResult
 * Package: com.yupi.yupicturebackend.model.dto.picture
 * Description:
 *      用于接受图片解析信息
 * @Author 阿小健
 * @Create 2025/4/25 15:28
 * @Version 1.0
 */
@Data
public class UploadPictureResult {
    /**
     * 图片地址
     */
    private String url;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 文件体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private int picWidth;

    /**
     * 图片高度
     */
    private int picHeight;

    /**
     * 图片宽高比
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;
}
