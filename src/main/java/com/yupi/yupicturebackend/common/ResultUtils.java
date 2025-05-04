package com.yupi.yupicturebackend.common;

import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import lombok.Data;

/**
 * ClassName: ResultUtils
 * Package: com.yupi.yupicturebackend.common
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/22 15:18
 * @Version 1.0
 */
public class ResultUtils {
    /**
     * 成功
     * @param data
     * @return
     * @param <T>
     */

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0,data, "ok");
    }

    /**
     * 失败
     * @param errorCode
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(), null, errorCode.getMessage());
    }

    /**
     * 失败
     * @param code
     * @param message
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> error(int code,String message) {
        return new BaseResponse<>(code,null, message);
    }

    /**
     * 失败
     * @param errorCode
     * @param message
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode,String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
}
