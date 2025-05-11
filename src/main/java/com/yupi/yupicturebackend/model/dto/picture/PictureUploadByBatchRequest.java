package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

/**
 * ClassName: PictureUploadByBatchRequest
 * Package: com.yupi.yupicturebackend.model.dto.picture
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/6 16:58
 * @Version 1.0
 */
@Data
public class PictureUploadByBatchRequest {
    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 3;

    /**
     * 名称前缀
     */
    private String namePrefix;

}
