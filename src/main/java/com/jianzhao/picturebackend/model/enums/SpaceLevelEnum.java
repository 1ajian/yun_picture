package com.jianzhao.picturebackend.model.enums;

import lombok.Getter;

/**
 * ClassName: SpaceLevelEnum
 * Package: com.yupi.yupicturebackend.model.enums
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/9 15:17
 * @Version 1.0
 */
@Getter
@SuppressWarnings("all")
public enum SpaceLevelEnum {

    COMMON("普通版", 0, 100, 100L * 1024 * 1024),
    PROFESSIONAL("专业版", 1, 1000, 1000L * 1024 * 1024),
    FLAGSHIP("旗舰版", 2, 10000, 10000L * 1024 * 1024);

    private final String text;

    private final int value;

    private final long maxCount;

    private final long maxSize;

    /**
     *
     * @param text 等级名称
     * @param value 值
     * @param maxCount 图片最大数
     * @param maxSize 图片最大容量
     */
    SpaceLevelEnum(String text,int value,long maxCount,long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 根据value获取枚举对象
     * @param value
     * @return
     */
    public static SpaceLevelEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }

        for (SpaceLevelEnum spaceLevelEnum : SpaceLevelEnum.values()) {
            if (value.equals(spaceLevelEnum.getValue())) {
                return spaceLevelEnum;
            }
        }

        return null;
    }

}
