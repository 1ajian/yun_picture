package com.yupi.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * ClassName: SpaceTypeEnum
 * Package: com.yupi.yupicturebackend.model.enums
 * Description:
 *  空间类型枚举
 * @Author 阿小健
 * @Create 2025/5/16 23:04
 * @Version 1.0
 */
@Getter
@SuppressWarnings("all")
public enum SpaceTypeEnum {

    PRIVATE("私有空间",0),
    TEAM("团队空间",1);

    private final String text;

    private final int value;

    SpaceTypeEnum(String text,int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举类
     * @param value
     * @return
     */
    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isNull(value)) {
            return null;
        }

        for (SpaceTypeEnum spaceTypeEnum : SpaceTypeEnum.values()) {
            if (spaceTypeEnum.value == value) {
                return spaceTypeEnum;
            }
        }

        return null;
    }
}
