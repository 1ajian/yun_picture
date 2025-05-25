package com.jianzhao.picturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ClassName: SpaceUserAnalyzeResponse
 * Package: com.yupi.yupicturebackend.model.vo.space.analyze
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/15 15:51
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceUserAnalyzeResponse implements Serializable {

    /**
     * 时间区间
     */
    private String period;

    /**
     * 上传数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}

