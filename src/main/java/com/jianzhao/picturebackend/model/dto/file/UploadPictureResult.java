package com.jianzhao.picturebackend.model.dto.file;

import com.qcloud.cos.model.ciModel.image.ImageLabelResponse;
import lombok.Data;

import java.util.List;

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
     * 缩略图 url
     */
    private String thumbnailUrl;


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

    /**
     * 原图url
     */
    private String originalUrl;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 包含标签信息的响应对象
     */
    private ImageLabelResponse pictureTagsResponse;

    /**
     * 分类信息
     */
    private List<String> categories;

}
