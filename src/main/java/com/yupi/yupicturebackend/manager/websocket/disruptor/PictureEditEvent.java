package com.yupi.yupicturebackend.manager.websocket.disruptor;

import com.yupi.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yupi.yupicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * ClassName: PictureEditEvent
 * Package: com.yupi.yupicturebackend.manager.websocket.disruptor
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/21 10:48
 * @Version 1.0
 */
@Data
public class PictureEditEvent {

    /**
     * 图片编辑请求消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前WebSocket的会话
     */
    private WebSocketSession webSocketSession;

    /**
     * 登录用户
     */
    private User user;

    /**
     * 图片Id
     */
    private Long pictureId;
}
