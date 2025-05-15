package com.yupi.yupicturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: CreateOutPaintingTaskRequest
 * Package: com.yupi.yupicturebackend.api.aliyunai.model
 * Description:
 *  创建AI扩展绘画任务请求参数
 * @Author 阿小健
 * @Create 2025/5/14 20:42
 * @Version 1.0
 */
@Data
public class CreateOutPaintingTaskRequest implements Serializable {

    private static final long serialVersionUID = 3560091816602166797L;
    // 主请求字段
    private String model = "image-out-painting";

    private Input input;

    private Parameters parameters;

    //--- 内部类：输入图像信息 ---
    @Data
    public static class Input {
        @Alias("image_url")
        private String imageUrl;
    }

    //--- 内部类：扩展参数 ---
    @Data
    public static class Parameters {
        private Integer angle; // 旋转角度 [0,359]

        @Alias("output_ratio")
        private String outputRatio; // 宽高比，如 "16:9"

        @Alias("x_scale")
        private Float xScale; // 水平扩展比例 [1.0, 3.0]

        @Alias("y_scale")
        private Float yScale; // 垂直扩展比例 [1.0, 3.0]

        @Alias("top_offset")
        private Integer topOffset; // 顶部扩展像素

        @Alias("bottom_offset")
        private Integer bottomOffset; // 底部扩展像素

        @Alias("left_offset")
        private Integer leftOffset; // 左侧扩展像素

        @Alias("right_offset")
        private Integer rightOffset; // 右侧扩展像素

        @Alias("best_quality")
        private Boolean bestQuality; // 是否开启高质量模式

        @Alias("limit_image_size")
        private Boolean limitImageSize; // 默认限制输出大小

        @Alias("add_watermark")
        private Boolean addWatermark = false; // 默认添加水印

    }


}
