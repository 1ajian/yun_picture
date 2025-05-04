package com.yupi.yupicturebackend.common;

import com.yupi.yupicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: BaseResponse
 * Package: com.yupi.yupicturebackend.common
 * Description:
 *      封装响应信息
 * @Author 阿小健
 * @Create 2025/4/22 15:13
 * @Version 1.0
 */

@Data
public class BaseResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 返回状态码
     */
    private int code;
    /**
     * 返回数据
     */
    private T data;

    /**
     * 返回信息
     */
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code,T data) {
        this(code,data,"");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(),null, errorCode.getMessage());
    }
}