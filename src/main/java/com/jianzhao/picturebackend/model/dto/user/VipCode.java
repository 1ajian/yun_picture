package com.jianzhao.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: VipCode
 * Package: com.jianzhao.picturebackend.model.dto.user
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/22 14:23
 * @Version 1.0
 */
@Data
public class VipCode implements Serializable {


    private static final long serialVersionUID = -4978021137546926428L;
    /**
     * 兑换码
     */
    private String vipCode;

    /**
     * 是否使用
     */
    private boolean hasUsed;

    /**
     * 1表示一个月,6表示半年,12表示一年
     */
    private Integer time;
}
