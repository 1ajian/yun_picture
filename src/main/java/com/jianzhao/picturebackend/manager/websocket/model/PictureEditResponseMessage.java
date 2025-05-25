package com.jianzhao.picturebackend.manager.websocket.model;

import com.jianzhao.picturebackend.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ClassName: PictureEditResponseMessage
 * Package: com.yupi.yupicturebackend.manager.websocket.model
 * Description:
 *  图片编辑响应消息类
 * @Author 阿小健
 * @Create 2025/5/20 15:39
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PictureEditResponseMessage implements Serializable {

    private static final long serialVersionUID = 2240290523657732796L;

    /**
     * 消息类型，例如 "INFO", "ERROR", "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 信息
     */
    private String message;

    /**
     * 执行的编辑动作
     */
    private String editAction;

    /**
     * 用户信息
     */
    private UserVO user;
}
