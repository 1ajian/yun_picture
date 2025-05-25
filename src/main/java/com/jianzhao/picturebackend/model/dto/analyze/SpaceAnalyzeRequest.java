package com.jianzhao.picturebackend.model.dto.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: SpaceAnalyzeRequest
 * Package: com.yupi.yupicturebackend.model.dto.analyze
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/15 12:36
 * @Version 1.0
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    /**
     * 全空间分析
     */
    private boolean queryAll;

    private static final long serialVersionUID = 1L;
}

