package com.jianzhao.picturebackend.manager.websocket.disruptor;

import com.jianzhao.picturebackend.manager.websocket.PictureEditHandler;
import com.jianzhao.picturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.jianzhao.picturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.jianzhao.picturebackend.model.entity.User;
import com.lmax.disruptor.WorkHandler;
import com.jianzhao.picturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.jianzhao.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * ClassName: PictureEditEventWorkHandler
 * Package: com.yupi.yupicturebackend.manager.websocket.disruptor
 * Description:
 *  `事件处理器(消费者)
 * @Author 阿小健
 * @Create 2025/5/21 10:52
 * @Version 1.0
 */
@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    /**
     * 事件消费者
     * @param pictureEditEvent
     * @throws Exception
     */
    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        WebSocketSession session = pictureEditEvent.getWebSocketSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();

        //获取枚举对象,根据编辑枚举对象
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);

        //Map<String, Object> attributes = session.getAttributes();
        //User user = (User) attributes.get("user");
        //Long pictureId = (Long) attributes.get("pictureId");

        //TODO:可以修改为策略模式
        switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage,session,user,pictureId);
                break;

            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;

            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;

            //若是其他消息,那么给当前会话进行一个报错处理
            default:
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUser(userService.getUserVO(user));
                String responseMessage = pictureEditHandler.getObjectMapper().writeValueAsString(pictureEditResponseMessage);
                session.sendMessage(new TextMessage(responseMessage));

        }
    }
}
