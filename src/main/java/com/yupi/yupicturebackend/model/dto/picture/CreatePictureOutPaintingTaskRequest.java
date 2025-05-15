package com.yupi.yupicturebackend.model.dto.picture;

import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: CreatePictureOutPaintingTaskRequest
 * Package: com.yupi.yupicturebackend.model.dto.picture
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/14 21:33
 * @Version 1.0
 */
@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    private static final long serialVersionUID = 6919483141995590501L;
    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;


}
