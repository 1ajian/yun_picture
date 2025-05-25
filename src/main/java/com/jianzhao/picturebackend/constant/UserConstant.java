package com.jianzhao.picturebackend.constant;

import java.util.UUID;

/**
 * ClassName: UserConstant
 * Package: com.yupi.yupicturebackend.constant
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/23 21:27
 * @Version 1.0
 */
public interface UserConstant {
    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * VIP用户
     */
    String VIP_ROLE = "VIP";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    // endregion

    //  region 用户信息
    String DEFAULT_PASSWORD = "12345678";

    //  endregion

    //  region redis缓存相关

    // 缓存用户图片验证码key
    String CAPTCHA_CODE_KEY = "user:login:captchaCode:";

    //  endregion
}
