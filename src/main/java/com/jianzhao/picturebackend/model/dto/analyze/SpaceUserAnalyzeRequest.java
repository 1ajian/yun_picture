package com.jianzhao.picturebackend.model.dto.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ClassName: SpaceUserAnalyzeRequest
 * Package: com.yupi.yupicturebackend.model.dto.analyze
 * Description:
 *     用户上传行为
 * @Author 阿小健
 * @Create 2025/5/15 15:49
 * @Version 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 时间维度：day / week / month
     */
    private String timeDimension;
}

