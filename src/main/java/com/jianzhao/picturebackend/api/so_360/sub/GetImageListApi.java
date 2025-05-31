package com.jianzhao.picturebackend.api.so_360.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jianzhao.picturebackend.api.so_360.model.ImageSearch360Result;
import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;

/**
 * ClassName: GetImageListApi
 * Package: com.jianzhao.picturebackend.api.so_360.sub
 * Description:
 *  获取图片列表API
 * @Author 阿小健
 * @Create 2025/5/26 14:26
 * @Version 1.0
 */
@Slf4j
public class GetImageListApi {

    public static List<ImageSearch360Result> getImageList(int start,String imgUrl) {
        String url = "https://st.so.com/stu?a=mrecomm&start=" + start;
        HashMap<String, Object> param = new HashMap<>();
        param.put("img_url", imgUrl);
        HttpResponse response = HttpRequest.post(url)
                .form(param)
                .timeout(5000)
                .execute();

        if (response.getStatus() != HttpStatus.HTTP_OK) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜图失败");
        }

        //解析响应结果
        JSONObject body = JSONUtil.parseObj(response.body());
        Integer code = (Integer) body.get("errno");
        if (!code.equals(0)) {
            throw  new BusinessException(ErrorCode.OPERATION_ERROR, "搜图失败");
        }

        JSONObject data = body.getJSONObject("data");
        //处理结果
        List<ImageSearch360Result> result = data.getBeanList("result", ImageSearch360Result.class);
        for (ImageSearch360Result imageSearch360Result : result) {
            String https = imageSearch360Result.getHttps();
            String http = imageSearch360Result.getHttp();
            String prefix = "";
            if (StrUtil.isNotBlank(https)) {
                prefix = "https://" + https + "/";
            } else if (StrUtil.isNotBlank(http)) {
                prefix = "http://" + http + "/";
            }
            imageSearch360Result.setSpareUrl(prefix + imageSearch360Result.getImgkey());
        }

        return result;
    }

    public static void main(String[] args) {
        List<ImageSearch360Result> imageList = GetImageListApi.getImageList(0, "https://ps.ssl.qhmsg.com/bdr/468_250_/t025ea002b9bd41dba6.jpg");
        System.out.println(imageList);
    }
}
