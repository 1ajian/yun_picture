package com.yupi.yupicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.yupi.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yupi.yupicturebackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * ClassName: PictureEditEventProducer
 * Package: com.yupi.yupicturebackend.manager.websocket.disruptor
 * Description:
 *      生产者：负责将事件发送到Disruptor环形缓冲区。保证停机时所有的消息都能被处理，通过shutdown方法完成Disruptor优雅停机
 * @Author 阿小健
 * @Create 2025/5/21 11:22
 * @Version 1.0
 */
@Component
@Slf4j
public class PictureEditEventProducer {

    @Resource
    private Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    /**
     * 发布消息
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
        //获取环形队列
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();

        //可以生成的位置（找位置）相当于拿序列号 （一个任务对应一个序列号：保证消息的顺序）
        long next = ringBuffer.next();
        //构建任务 （存入任务） 根据序列号拿位置
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        pictureEditEvent.setPictureId(pictureId);
        pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setWebSocketSession(session);
        //将序列号 发布
        ringBuffer.publish(next);
    }

    @PreDestroy
    public void close() {
        //优雅关闭disruptor
        pictureEditEventDisruptor.shutdown();
    }
}
