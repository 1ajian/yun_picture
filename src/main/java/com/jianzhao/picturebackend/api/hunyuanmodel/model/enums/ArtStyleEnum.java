package com.jianzhao.picturebackend.api.hunyuanmodel.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * ClassName: ArtStyle
 * Package: com.jianzhao.picturebackend.api.hunyuanmodel.model
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/26 16:16
 * @Version 1.0
 */
@Getter
@SuppressWarnings("all")
public enum ArtStyleEnum {

    RIMAN("日漫动画", "riman"),
    SHUIMO("水墨画", "shuimo"),
    MONAI("莫奈", "monai"),
    BIANPING("扁平插画", "bianping"),
    XIANGSU("像素插画", "xiangsu"),
    ERTONGHUBEN("儿童绘本", "ertonghuiben"),
    THREE_D_XUANRAN("3D渲染", "3dxuanran"),
    MANHUA("漫画", "manhua"),
    HEIBAIMANHUA("黑白漫画", "heibaimanhua"),
    XIESHI("写实", "xieshi"),
    DONGMAN("动漫", "dongman"),
    BIJIASUO("毕加索", "bijiasuo"),
    SAIBOPENGKE("赛博朋克", "saibopengke"),
    YOUHUA("油画", "youhua"),
    MASAIKE("马赛克", "masaike"),
    QINGHUACI("青花瓷", "qinghuaci"),
    XINNIANJIANZHI("新年剪纸画", "xinnianjianzhi"),
    XINNIANHUAYI("新年花艺", "xinnianhuayi");

    private final String text;
    private final String value;

    ArtStyleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 编写使用text获取枚举对象方法
     * @param text
     * @return
     */
    public static ArtStyleEnum getArtStyleByText(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        for (ArtStyleEnum artStyleEnum : ArtStyleEnum.values()) {
            if (artStyleEnum.getText().equals(text)) {
                return artStyleEnum;
            }
        }
        return null;
    }
}
