package com.yupi.yupicturebackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * ClassName: SpaceLevel
 * Package: com.yupi.yupicturebackend.model.vo
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/9 21:15
 * @Version 1.0
 */
@Data
@AllArgsConstructor
public class SpaceLevel {
    private int value;

    private String text;

    private long maxCount;

    private long maxSize;
}
