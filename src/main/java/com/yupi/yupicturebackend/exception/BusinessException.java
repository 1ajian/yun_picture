package com.yupi.yupicturebackend.exception;

import lombok.Getter;

/**
 * ClassName: BusinessException
 * Package: com.yupi.yupicturebackend.exception
 * Description:
 *  自定义异常类
 * @Author 阿小健
 * @Create 2025/4/22 14:40
 * @Version 1.0
 */
@Getter
public class BusinessException extends RuntimeException{
    /**
     * 异常码
     */
    private int code;

    public BusinessException(int code,String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode,String message) {
        this(errorCode.getCode(), message);
    }

}
