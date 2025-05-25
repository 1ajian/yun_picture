package com.jianzhao.picturebackend.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ClassName: PictureEditRequestMessage
 * Package: com.yupi.yupicturebackend.manager.websocket.model
 * Description:
 *  图片编辑请求消息类
 * @Author 阿小健
 * @Create 2025/5/20 15:37
 * @Version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditRequestMessage implements Serializable {

    private static final long serialVersionUID = -5160062250088639461L;

    /**
     * 消息类型，例如 "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 执行的编辑动作
     */
    private String editAction;
}
