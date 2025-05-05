package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: PictureReviewRequest
 * Package: com.yupi.yupicturebackend.model.dto.picture
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/4 20:22
 * @Version 1.0
 */
@Data
public class PictureReviewRequest implements Serializable {

    private static final long serialVersionUID = -451991670421981642L;

    /**
     * id
     */
    private Long id;

    /**
     * 状态：0-待审核, 1-通过, 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;
}
