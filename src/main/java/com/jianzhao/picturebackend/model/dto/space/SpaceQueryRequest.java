package com.jianzhao.picturebackend.model.dto.space;

import com.jianzhao.picturebackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: SpaceQueryRequest
 * Package: com.yupi.yupicturebackend.model.dto.space
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/9 15:08
 * @Version 1.0
 */

@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;


    private static final long serialVersionUID = 1L;
}
