package com.jianzhao.picturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.jianzhao.picturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.jianzhao.picturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.jianzhao.picturebackend.model.entity.User;
import com.jianzhao.picturebackend.model.vo.UserVO;
import com.jianzhao.picturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.jianzhao.picturebackend.manager.websocket.model.PictureEditActionEnum;
import com.jianzhao.picturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.jianzhao.picturebackend.service.UserService;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassName: PictureEditHandler
 * Package: com.yupi.yupicturebackend.manager.websocket
 * Description:
 *  WebSocket处理器
 * @Author 阿小健
 * @Create 2025/5/20 16:23
 * @Version 1.0
 */
@Component
@SuppressWarnings("all")
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    //引入生产者
    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    //key:pictureId     value:userId 表示正在编辑图片的用户
    private final Map<Long,Long> pictureEditingUsers = new ConcurrentHashMap<>();

    //key:pictureId  value:session集合 表示加入编辑的会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * ws建立连接之后
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //保存会话到session
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long userId = (Long) attributes.get("userId");
        Long pictureId = (Long) attributes.get("pictureId");
        //如果当前picture -> 会话集合不存在,就创建一个新的，并且返回null
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        //把当前建立连接会话加入进去
        pictureSessions.get(pictureId).add(session);

        //构造响应
        String type = PictureEditMessageTypeEnum.INFO.getValue();
        String message = String.format("%s加入编辑",user.getUserName());
        UserVO userVO = userService.getUserVO(user);
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setType(type);
        pictureEditResponseMessage.setUser(userVO);
        //广播
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 接收客户端的消息,根据不同的消息类型进行处理
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        //防止Id出现错误(不过也不影响) 可以直接使用JSONUtil
        String pictureEditRequestMessageJSON = message.getPayload();
        ObjectMapper objectMapper = getObjectMapper();
        PictureEditRequestMessage pictureEditRequestMessage = objectMapper.readValue(pictureEditRequestMessageJSON, PictureEditRequestMessage.class);

        //获取枚举对象,根据编辑枚举对象
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);

        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");

        //调用生产者
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage,session, user, pictureId);

        //TODO:可以修改为策略模式
//        switch (pictureEditMessageTypeEnum) {
//            case ENTER_EDIT:
//                handleEnterEditMessage(pictureEditRequestMessage,session,user,pictureId);
//                break;
//
//            case EDIT_ACTION:
//                handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
//                break;
//
//            case EXIT_EDIT:
//                handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
//                break;
//
//            //若是其他消息,那么给当前会话进行一个报错处理
//            default:
//                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
//                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
//                pictureEditResponseMessage.setMessage("消息类型错误");
//                pictureEditResponseMessage.setUser(userService.getUserVO(user));
//                String responseMessage = objectMapper.writeValueAsString(pictureEditResponseMessage);
//                session.sendMessage(new TextMessage(responseMessage));

//        }

    }

    /**
     * 处理编辑进入消息
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
        //判断如果当前图片没有用户在进行编辑
        if (pictureEditingUsers.get(pictureId) == null) {
            pictureEditingUsers.put(pictureId, user.getId());

            //广播session
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            pictureEditResponseMessage.setMessage(String.format("%s开始编辑图片", user.getUserName()));
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }


    /**
     * 处理编辑动作消息
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
        //正在编辑的用户Id
        Long editUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        //获取枚举类型
        PictureEditActionEnum pictureEditActionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (pictureEditActionEnum == null) {
            return;
        }

        //要确定编辑者是当前session的用户
        if (editUserId != null && editUserId.equals(user.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setMessage(String.format("%s执行了%s", user.getUserName(),pictureEditActionEnum.getText()));
            //广播给其他人,防止自己重复编辑
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 处理编辑退出消息（用户退出编辑操作）
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
        Long editUserId = pictureEditingUsers.get(pictureId);
        //如果是当前会话的用户
        if (editUserId != null && user.getId().equals(editUserId)) {
            //移除正在编辑图片的用户
            pictureEditingUsers.remove(pictureId);
            //构建返回消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            pictureEditResponseMessage.setMessage(String.format("%s退出编辑图片", user.getUserName()));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * websocket 关闭连接,移除当前用户的编辑状态、并且从集合中删除当前会话,给其他客户端发送消息
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");

        //移除当前用户的编辑状态
        this.handleExitEditMessage(null,session,user, pictureId);

        //从集合中删除当前会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }

        //给其他客户端发送消息
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setMessage(String.format("%s离开编辑", user.getUserName()));
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        broadcastToPicture(pictureId, pictureEditResponseMessage, session);

    }

    /**
     * 广播方法 (有排除功能)
     * @param pictureId 图片Id
     * @param pictureEditResponseMessage 图片编辑响应信息
     * @param excludeSession 排除掉的会话
     * @throws Exception
     */
    @SneakyThrows
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession){
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);

        if (CollUtil.isNotEmpty(sessionSet)) {
            ObjectMapper objectMapper = getObjectMapper();
            //序列化为JSON字符串
            String responseMessage = objectMapper.writeValueAsString(pictureEditResponseMessage);

            TextMessage textMessage = new TextMessage(responseMessage);

            for (WebSocketSession session : sessionSet) {
                //排除掉不需要传递的session
                if (sessionSet.contains(excludeSession)) {
                    continue;
                }

                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }

        }

    }

    /**
     * 为了保证Long类型(long)序列化正确,我们自定义序列化器 jackson
     * @return
     */
    public ObjectMapper getObjectMapper() {
        // 创建 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
        objectMapper.registerModule(module);
        return objectMapper;
    }

    /**
     * 广播方法,无排除功能
     * @param pictureId
     * @param pictureEditResponseMessage
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) {
        broadcastToPicture(pictureId,pictureEditResponseMessage);
    }



}
