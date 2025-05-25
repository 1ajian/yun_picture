package com.jianzhao.picturebackend.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ClassName: SpaceRoleEnum
 * Package: com.yupi.yupicturebackend.model.enums
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/16 23:44
 * @Version 1.0
 */
@Getter
@SuppressWarnings("all")
public enum SpaceRoleEnum {

    VIEWER("浏览者","viewer"),
    EDITOR("编辑者","editor"),
    ADMIN("管理员","admin");

    private final String text;

    private final String value;

    SpaceRoleEnum(String text,String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举类对象
     * @param value
     * @return
     */
    public static SpaceRoleEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        for (SpaceRoleEnum spaceRoleEnum : SpaceRoleEnum.values()) {
            if (spaceRoleEnum.value.equals(value)) {
                return spaceRoleEnum;
            }
        }
        return null;
    }

    /**
     * 返回所有枚举的文本列表
     * @return
     */
    public static List<String> getAllTexts() {
        List<String> texts = Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getText).collect(Collectors.toList());
        return texts;
    }

    /**
     * 返回所有枚举的值列表
     * @return
     */
    public static List<String> getAllValues() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getValue)
                .collect(Collectors.toList());
    }
}
