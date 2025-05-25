package com.jianzhao.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ClassName: PictureTagCategory
 * Package: com.yupi.yupicturebackend.model.vo
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/26 17:09
 * @Version 1.0
 */
@Data
public class PictureTagCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    List<String> tagList;
    List<String> categoryList;
}
