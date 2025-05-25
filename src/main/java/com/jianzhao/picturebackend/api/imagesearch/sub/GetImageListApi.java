package com.jianzhao.picturebackend.api.imagesearch.sub;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jianzhao.picturebackend.api.imagesearch.model.ImageSearchResult;
import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import com.jianzhao.picturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * ClassName: GetImageListApi
 * Package: com.yupi.yupicturebackend.api.imagesearch.sub
 * Description:
 * 获取图片列表
 * @Author 阿小健
 * @Create 2025/5/13 11:09
 * @Version 1.0
 */
@Slf4j
public class GetImageListApi {

    /**
     * 获取图片列表
     * @param url
     * @return
     */
    public static List<ImageSearchResult> getImageList(String url) {
        try {
            HttpResponse response = HttpUtil.createGet(url).execute();

            int statusCode = response.getStatus();
            if (statusCode == HttpStatus.HTTP_OK) {
                return processResponse(response.body());
            } else {
                log.error("获取图片列表接口调用失败");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
        } catch (Exception e) {
            log.error("获取图片列表失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }
    }

    /**
     * 处理结果
     * @param responseBody
     * @return
     */
    private static List<ImageSearchResult> processResponse(String responseBody) {
        JSONObject jsonObject = new JSONObject(responseBody);

        int status = (int) jsonObject.get("status");
        ThrowUtils.throwIf(status != 0,ErrorCode.OPERATION_ERROR,"请求失败");

        JSONObject data = jsonObject.getJSONObject("data");
        JSONArray list = data.getJSONArray("list");
        ThrowUtils.throwIf(CollUtil.isEmpty(list), ErrorCode.OPERATION_ERROR, "未获取到图片列表");

        return JSONUtil.toList(list, ImageSearchResult.class);
    }

    public static void main(String[] args) {
        List<ImageSearchResult> imageList = GetImageListApi.getImageList("https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=14079056900383585531&sign=126584d2788b75492145301747104945&tk=487fc&tpl_from=pc");
        System.out.println(imageList);
    }
}
