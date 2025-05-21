package com.yupi.yupicturebackend.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.dsl.Disruptor;
import groovy.lang.GrabConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * ClassName: PictureEditEventDisruptorConfig
 * Package: com.yupi.yupicturebackend.manager.websocket.disruptor
 * Description:
 *  事件处理的配置类   Disruptor 配置类(需要配置 环形队列)包含消费者、生产者、队列等等
 *  用于异步处理消息   Disruptor简单理解就是一个环形队列
 * @Author 阿小健
 * @Create 2025/5/21 11:08
 * @Version 1.0
 */
@Configuration
public class PictureEditEventDisruptorConfig {
    //消费者
    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {
        //设置环形队列大小
        int ringBufferSize = 256 * 1024;

        //构建环形队列
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(PictureEditEvent::new
                , ringBufferSize, ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build());

        //设置消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        return disruptor;
    }
}
