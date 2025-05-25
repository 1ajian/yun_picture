package com.jianzhao.picturebackend.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * ClassName: StpKit
 * Package: com.yupi.yupicturebackend.manager.auth
 * Description:
 *  StpLogic 门面类，管理项目中所有的 StpLogic 账号体系
 *  添加 @Component 注解的目的是确保静态属性 DEFAULT 和 SPACE 被初始化
 * @Author 阿小健
 * @Create 2025/5/17 16:12
 * @Version 1.0
 */
@Component
public class StpKit {

    /**
     * 类型
     */
    public static final String SPACE_TYPE = "space";

    /**
     * 默认原生会话对象，项目中目前没使用到
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    /**
     * Space 会话对象，管理 Space 表所有账号的登录、权限认证
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);

}
