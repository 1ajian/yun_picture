package com.jianzhao.picturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName: UserRoleEnum
 * Package: com.yupi.yupicturebackend.model.enums
 * Description:
 *      用户角色枚举类
 * @Author 阿小健
 * @Create 2025/4/23 20:09
 * @Version 1.0
 */
@Getter
public enum UserRoleEnum {
    /**
     * 所有的枚举对象
     */
    USER("用户","user"),
    VIP("会员","VIP"),
    ADMIN("管理员","admin");

    private final String text;

    private final String value;

    UserRoleEnum(String text,String value) {
        this.text = text;
        this.value = value;
    }

    //静态Map缓存所有枚举值 (key是value,value是枚举实例)
    private static final Map<String,UserRoleEnum> VALUE_MAP = new HashMap<>();

    //静态代码块，初始化静态Map
    static {
        for (UserRoleEnum role : UserRoleEnum.values()) {
            VALUE_MAP.put(role.value, role);
        }
    }

    /**
     * 根据value值获取枚举对象
     * @param value 枚举对象的value
     * @return 用户身份枚举对象
     */
    public static UserRoleEnum getEnumByValue(String value) {
        //判空
        if (ObjUtil.isEmpty(value)) {
            return null;
        }


        /* 代码优化：如果枚举值特别多，可以 Map 缓存所有枚举值来加速查找，而不是遍历列表。
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (value.equals(userRoleEnum.value)) {
                return userRoleEnum;
            }
        }
        return null;
        */

        return VALUE_MAP.get(value);
    }
}
