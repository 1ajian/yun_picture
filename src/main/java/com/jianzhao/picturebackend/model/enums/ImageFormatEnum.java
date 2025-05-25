package com.jianzhao.picturebackend.model.enums;

import lombok.Getter;

/**
 * ClassName: ImageFormatEnum
 * Package: com.yupi.yupicturebackend.model.enums
 * Description:
 *      定义图片类型枚举类
 * @Author 阿小健
 * @Create 2025/4/26 14:01
 * @Version 1.0
 */
@Getter
public enum ImageFormatEnum {
    /**
     * 所有的枚举对象
     */
    JPEG("jpeg", "image/jpeg"),
    JPG("jpg", "image/jpg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp"),
    GIF("gif", "image/gif");

    //文件扩展名
    private final String extension;
    //对应的MIME类型
    private final String mimeType;

    ImageFormatEnum(String extension,String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    //根据extension获取枚举类型
    public static ImageFormatEnum getImageFormatEnumBySuffix(String extension) {
        for (ImageFormatEnum imageFormatEnum : ImageFormatEnum.values()) {
            if (imageFormatEnum.extension.equals(extension)) {
                return imageFormatEnum;
            }
        }

        return null;
    }

    /**
     * 根据媒体类型返回枚举类
     * @param mimeType
     * @return
     */
    public static ImageFormatEnum checkMimeType(String mimeType) {
        for (ImageFormatEnum imageFormatEnum : ImageFormatEnum.values()) {
            if (imageFormatEnum.mimeType.equals(mimeType)) {
                return imageFormatEnum;
            }
        }
        return null;
    }
}
