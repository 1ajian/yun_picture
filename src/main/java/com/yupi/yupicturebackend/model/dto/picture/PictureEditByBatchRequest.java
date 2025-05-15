package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ClassName: PictureEditByBatchRequest
 * Package: com.yupi.yupicturebackend.model.dto.picture
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/14 9:25
 * @Version 1.0
 */
@Data
public class PictureEditByBatchRequest implements Serializable {

    private static final long serialVersionUID = -4982514299457706082L;
    /**
     * 图片Id列表
     */
    private List<Long> pictureIdList;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 命名规则
     */
    private String nameRule;


}
