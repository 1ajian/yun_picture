package com.jianzhao.picturebackend.api.hunyuanmodel.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * ClassName: SubmitImageJobResponse
 * Package: com.jianzhao.picturebackend.api.hunyuanmodel.model
 * Description:
 *  提交结果类
 * @Author 阿小健
 * @Create 2025/5/26 17:22
 * @Version 1.0
 */
@Data
public class SubmitImageJobResponse {

    @SerializedName("JobId")
    private String jobId;

    /**
     * 唯一请求 ID，由服务端生成，每次请求都会返回（若请求因其他原因未能抵达服务端，则该次请求不会获得 RequestId）。定位问题时需要提供该次请求的 RequestId。
     */
    @SerializedName("RequestId")
    private String requestId;
}
