package com.jianzhao.picturebackend.api.hunyuanmodel;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jianzhao.picturebackend.api.hunyuanmodel.model.HunyuanImageResponse;
import com.jianzhao.picturebackend.api.hunyuanmodel.model.SubmitImageJobRequest;
import com.jianzhao.picturebackend.api.hunyuanmodel.model.enums.ArtStyleEnum;
import com.jianzhao.picturebackend.config.CosClientConfig;
import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import com.jianzhao.picturebackend.exception.ThrowUtils;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.hunyuan.v20230901.HunyuanClient;
import com.tencentcloudapi.hunyuan.v20230901.models.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;

/**
 * ClassName: TencentAIPaintingApi
 * Package: com.jianzhao.picturebackend.api.hunyuanmodel
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/26 16:50
 * @Version 1.0
 */
@Component
@Slf4j
public class TencentAIPaintingApi {

    @Resource
    private CosClientConfig cosClientConfig;

    String ENDPOINT = "hunyuan.tencentcloudapi.com";

    /**
     * 提交任务
     * @param request
     * @return
     */
    public SubmitHunyuanImageJobResponse submitHunyuanImageJob(SubmitImageJobRequest request) {
        HunyuanClient client = getClient();
        //获取封装的请求参数
        SubmitHunyuanImageJobRequest req = getReq(request);

        // 返回的resp是一个SubmitHunyuanImageJobResponse的实例，与请求对象对应
        SubmitHunyuanImageJobResponse resp = null;
        try {
            resp = client.SubmitHunyuanImageJob(req);
        } catch (TencentCloudSDKException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"提交任务失败!");
        }

        return resp;
    }

    private HunyuanClient getClient() {
        Credential cred = new Credential(cosClientConfig.getSecretId(), cosClientConfig.getSecretKey());

        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(ENDPOINT);

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        HunyuanClient client = new HunyuanClient(cred, cosClientConfig.getRegion(), clientProfile);
        return client;

    }

    /**
     * 封装请求参数
     * @param request
     * @return
     */
    private SubmitHunyuanImageJobRequest getReq(SubmitImageJobRequest request) {

        // 实例化一个请求对象,每个接口都会对应一个request对象
        SubmitHunyuanImageJobRequest req = new SubmitHunyuanImageJobRequest();

        if (StrUtil.isBlank(request.getPrompt())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请输入提示词!");
        }
        req.setPrompt(request.getPrompt());

        String text = request.getStyle();
        ArtStyleEnum style = ArtStyleEnum.getArtStyleByText(text);
        if (style != null) {
            req.setStyle(style.getValue());
        }

        req.setResolution(request.getResolution());
        Image image = new Image();

        String imgUrl = request.getImgUrl();
        String base64Url = request.getBase64Url();
        if (StrUtil.isNotBlank(imgUrl) && StrUtil.isBlank(base64Url)) {
            image.setImageUrl(imgUrl);
        } else if (StrUtil.isNotBlank(base64Url) && StrUtil.isBlank(imgUrl)){
            image.setImageBase64(base64Url);
        }

        if (request.getRevise() != null) {
            req.setRevise(request.getRevise().longValue());
        }

        if (request.getSeed() != null) {
            req.setSeed(request.getSeed().longValue());
        }
        req.setLogoAdd(0L);
        return req;
    }

    /**
     * 获取图片任务结果
     * @param jobId
     * @return
     */
    public QueryHunyuanImageJobResponse getImageJob(String jobId) {
        HunyuanClient client = getClient();
        // 实例化一个请求对象,每个接口都会对应一个request对象
        QueryHunyuanImageJobRequest req = new QueryHunyuanImageJobRequest();
        req.setJobId(jobId);
        // 返回的resp是一个QueryHunyuanImageJobResponse的实例，与请求对象对应
        QueryHunyuanImageJobResponse resp = null;
        try {
            resp = client.QueryHunyuanImageJob(req);
        } catch (TencentCloudSDKException e) {
            log.error("获取图片任务结果失败:" + e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取图片任务结果失败!");
        }
        // 输出json格式的字符串回包
        return resp;
    }
}
