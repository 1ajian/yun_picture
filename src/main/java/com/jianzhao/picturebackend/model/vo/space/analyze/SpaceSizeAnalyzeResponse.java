package com.jianzhao.picturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ClassName: SpaceSizeAnalyzeResponse
 * Package: com.yupi.yupicturebackend.model.vo.space.analyze
 * Description:
 * 空间图片大小分析相应类
 * @Author 阿小健
 * @Create 2025/5/15 15:23
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceSizeAnalyzeResponse implements Serializable {

    /**
     * 图片大小范围
     */
    private String sizeRange;

    /**
     * 图片数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
