package com.jianzhao.picturebackend.api.so_360.model;

import lombok.Data;

/**
 * ClassName: SoImageSearchRequest
 * Package: com.jianzhao.picturebackend.api.so_360.model
 * Description:
 *  360图片搜索结果
 * @Author 阿小健
 * @Create 2025/5/26 13:55
 * @Version 1.0
 */
@Data
public class ImageSearch360Result {
    /**
     * 图片地址
     */
    private String imgurl;

    /**
     * 标题
     */
    private String title;

    /**
     * 图片key
     */
    private String imgkey;

    /**
     * 站点
     */
    private String site;

    /**
     * HTTP
     */
    private String http;

    /**
     * HTTPS
     */
    private String https;

    /**
     * 备用的图片地址
     */
    private String spareUrl;
}
