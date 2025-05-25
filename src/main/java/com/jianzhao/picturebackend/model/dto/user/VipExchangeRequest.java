package com.jianzhao.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: VipExchangeRequest
 * Package: com.jianzhao.picturebackend.model.dto.user
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/22 16:18
 * @Version 1.0
 */
@Data
public class VipExchangeRequest implements Serializable {

    private static final long serialVersionUID = 5973155613686375560L;

    /**
     * 兑换码
     */
    private String vipCode;
}
