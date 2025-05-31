package com.jianzhao.picturebackend.api.hunyuanmodel.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.jianzhao.picturebackend.api.hunyuanmodel.model.enums.ArtStyleEnum;
import com.tencentcloudapi.hunyuan.v20230901.models.Image;
import com.tencentcloudapi.hunyuan.v20230901.models.LogoParam;
import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: SubmitImageJobRequest
 * Package: com.jianzhao.picturebackend.api.hunyuanmodel.model
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/26 16:58
 * @Version 1.0
 */
@Data
public class SubmitImageJobRequest implements Serializable {

    private static final long serialVersionUID = -3332891798993130839L;
    /**
     * 描述
     */
    private String prompt;

    /**
     * 风格
     */
    private String style;

    /**
     * 生成图分辨率。
     支持生成以下分辨率的图片：768:768（1:1）、768:1024（3:4）、1024:768（4:3）、1024:1024（1:1）、720:1280（9:16）、1280:720（16:9）、768:1280（3:5）、1280:768（5:3），不传默认使用1024:1024。
     如果上传 ContentImage 参考图，分辨率仅支持：768:768（1:1）、768:1024（3:4）、1024:768（4:3）、1024:1024（1:1），不传将自动适配分辨率。如果参考图被用于做风格转换，将生成保持原图长宽比例且长边为1024的图片，指定的分辨率不生效。
     */
    private String resolution;

    /**
     * 参考图URL
     */
    private String imgUrl;

    /**
     * 参考图Base64
     */
    private String base64Url;

    /**
     * prompt 扩写开关。1为开启，0为关闭，不传默认开启。
     */
    private Integer revise = 0;

    /**
     * 随机种子 只有正数和不传
     */
    private Integer seed = null;


}
