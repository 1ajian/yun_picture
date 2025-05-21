package com.yupi.yupicturebackend.manager.websocket.model;

import lombok.Getter;

/**
 * ClassName: PictureEditActionEnum
 * Package: com.yupi.yupicturebackend.manager.websocket.model
 * Description:
 *      图片的编辑操作枚举类
 * @Author 阿小健
 * @Create 2025/5/20 15:47
 * @Version 1.0
 */
@Getter
@SuppressWarnings("all")
public enum PictureEditActionEnum {
    ZOOM_IN("放大操作", "ZOOM_IN"),
    ZOOM_OUT("缩小操作", "ZOOM_OUT"),
    ROTATE_LEFT("左旋操作", "ROTATE_LEFT"),
    ROTATE_RIGHT("右旋操作", "ROTATE_RIGHT");

    private final String text;
    private final String value;

    PictureEditActionEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PictureEditActionEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (PictureEditActionEnum actionEnum : PictureEditActionEnum.values()) {
            if (actionEnum.value.equals(value)) {
                return actionEnum;
            }
        }
        return null;
    }
}
