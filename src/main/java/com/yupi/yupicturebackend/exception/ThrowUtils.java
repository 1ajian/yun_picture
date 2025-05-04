package com.yupi.yupicturebackend.exception;

/**
 * ClassName: ThrowUtil
 * Package: com.yupi.yupicturebackend.exception
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/22 14:46
 * @Version 1.0
 */
public class ThrowUtils {

    /**
     * 条件成立立即抛出
     * @param condition
     * @param exception
     */
    public static void throwIf(boolean condition,RuntimeException exception) {
        if (condition) {
            throw exception;
        }

    }

    /**
     * 条件成立，立即抛出
     * @param condition
     * @param errorCode
     */
    public static void throwIf(boolean condition,ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));

    }

    /**
     *  条件成立，立即抛出
     * @param condition
     * @param errorCode
     * @param message
     */
    public static void throwIf(boolean condition,ErrorCode errorCode,String message) {
        throwIf(condition,new BusinessException(errorCode,message));

    }
}