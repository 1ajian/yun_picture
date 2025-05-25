package com.jianzhao.picturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: DeleteRequest
 * Package: com.yupi.yupicturebackend.common
 * Description:
 *      通用删除请求类
 * @Author 阿小健
 * @Create 2025/4/22 16:11
 * @Version 1.0
 */
@Data
public class DeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;


}
