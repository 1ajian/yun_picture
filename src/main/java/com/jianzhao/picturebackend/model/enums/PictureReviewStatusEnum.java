package com.jianzhao.picturebackend.model.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName: PictureReviewStatusEnum
 * Package: com.yupi.yupicturebackend.model.enums
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/4 20:07
 * @Version 1.0
 */
@Getter
@SuppressWarnings("all")
public enum PictureReviewStatusEnum {
    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);

    private final String text;

    private final Integer value;

    private PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    private static final Map<Integer,PictureReviewStatusEnum> VALUE_MAP = new HashMap<>();

    static {
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            VALUE_MAP.put(pictureReviewStatusEnum.value,pictureReviewStatusEnum);
        }
    }

    /**
     * 根据value获取枚举对象
     * @param value
     * @return
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {
        if (value != null) {
            return null;
        }

        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if (pictureReviewStatusEnum.value == value) {
                return pictureReviewStatusEnum;
            }

        }

        return null;
    }

}
