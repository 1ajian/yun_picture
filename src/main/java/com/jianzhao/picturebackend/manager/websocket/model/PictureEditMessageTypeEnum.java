package com.jianzhao.picturebackend.manager.websocket.model;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * ClassName: PictureEditMessageTypeEnum
 * Package: com.yupi.yupicturebackend.manager.websocket.model
 * Description:
 *  图片编辑消息类型枚举类
 * @Author 阿小健
 * @Create 2025/5/20 15:42
 * @Version 1.0
 */
@Getter
@SuppressWarnings("all")
public enum PictureEditMessageTypeEnum {
    INFO("发送消息","INFO"),
    ERROR("发送错误", "ERROR"),
    ENTER_EDIT("进入编辑状态", "ENTER_EDIT"),
    EXIT_EDIT("退出编辑状态", "EXIT_EDIT"),
    EDIT_ACTION("执行编辑操作", "EDIT_ACTION");

    private final String text;

    private final String value;

    PictureEditMessageTypeEnum(String text,String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取对应的编辑消息类型对象
     * @param value
     * @return
     */
    public static PictureEditMessageTypeEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        for (PictureEditMessageTypeEnum pictureEditMessageTypeEnum : PictureEditMessageTypeEnum.values()) {
            if (pictureEditMessageTypeEnum.getValue().equals(value)) {
                return pictureEditMessageTypeEnum;
            }

        }

        return null;
    }
}
