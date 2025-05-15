package com.yupi.yupicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * ClassName: AliYunAiApi
 * Package: com.yupi.yupicturebackend.api.aliyunai
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/14 21:07
 * @Version 1.0
 */
@Data
@Slf4j
@Component
@ConfigurationProperties("aliyunai")
public class AliYunAiApi {
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 发送请求获取任务执行Id
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        //校验请求参数
        Optional.ofNullable(createOutPaintingTaskRequest).orElseThrow(()-> {throw new BusinessException(ErrorCode.PARAMS_ERROR);});

        createOutPaintingTaskRequest.getParameters().setAddWatermark(false);
        String jsonRequest = JSONUtil.toJsonStr(createOutPaintingTaskRequest);
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .body(jsonRequest);

        try(HttpResponse httpresponse = httpRequest.execute()) {
            if (!httpresponse.isOk()) {
                log.error("请求异常：{}", httpresponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }

            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpresponse.body(), CreateOutPaintingTaskResponse.class);

            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                log.error("AI 扩图失败，errorCode:{}, errorMessage:{}", errorCode, response.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            return response;

        }
    }

    /**
     * 根据Id发起请求查看状态并获取结果
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), new BusinessException(ErrorCode.PARAMS_ERROR, "任务Id不能为空"));

        try(HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header("Authorization", "Bearer " + apiKey).execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }

            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
