package com.yupi.yupicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ClassName: SpaceTagAnalyzeResponse
 * Package: com.yupi.yupicturebackend.model.vo.space.analyze
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/15 14:55
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTagAnalyzeResponse implements Serializable {

    /**
     * 标签名称
     */
    private String tag;

    /**
     * 使用次数
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}

