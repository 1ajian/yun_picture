package com.jianzhao.picturebackend.api.hunyuanmodel.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ClassName: HunyuanImageResponse
 * Package: com.jianzhao.picturebackend.api.hunyuanmodel.model
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/26 17:51
 * @Version 1.0
 */
@Data
public class HunyuanImageResponse implements Serializable {

    private static final long serialVersionUID = 6753910028159835742L;
    @SerializedName("Response")
    private ResponseData response;

    /**
     * 响应数据内部类
     */
    @Data
    public static class ResponseData {
        @SerializedName("JobErrorCode")
        private String jobErrorCode;

        @SerializedName("JobErrorMsg")
        private String jobErrorMsg;

        @SerializedName("JobStatusCode")
        private String jobStatusCode;

        @SerializedName("JobStatusMsg")
        private String jobStatusMsg;

        @SerializedName("RequestId")
        private String requestId;

        @SerializedName("ResultDetails")
        private List<String> resultDetails;

        @SerializedName("ResultImage")
        private List<String> resultImageUrls;

        @SerializedName("RevisedPrompt")
        private List<String> revisedPrompts;
    }
}
