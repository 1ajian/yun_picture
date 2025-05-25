package com.jianzhao.picturebackend.model.dto.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: SpaceRankAnalyzeRequest
 * Package: com.yupi.yupicturebackend.model.dto.analyze
 * Description:
 *  空间使用排行请求类
 * @Author 阿小健
 * @Create 2025/5/15 16:20
 * @Version 1.0
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;

    private static final long serialVersionUID = 1L;
}

