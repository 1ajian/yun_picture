package com.yupi.yupicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.test.BaiduImageSearcher;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName: GetImagePageUrlApi
 * Package: com.yupi.yupicturebackend.api.imagesearch.sub
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/12 22:09
 * @Version 1.0
 */
public class GetImagePageUrlApi {

    /**
     * 获取图片页面地址
     * @param imageUrl 以图搜图的url
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {

        HashMap<String, Object> formData = new HashMap<>();
        formData.put("image",imageUrl);
        formData.put("tn", "pc");
        formData.put("from","pc");
        formData.put("image_source", "PC_UPLOAD_URL");

        long uptime = System.currentTimeMillis();
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        HttpResponse response;
        try {
            //发送请求
            response = HttpRequest.post(url)
                    .form(formData)
                    .header("Acs-Token", RandomUtil.randomString(25))
                    .timeout(5000)
                    .execute();

            //验证
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

            //解析响应
            String responseBody = response.body();
            Map<String,Object> result = JSONUtil.toBean(responseBody, Map.class);

            //处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            //转为map方便获取
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            //对url进行解码
            String searchResultUrl = URLUtil.decode(rawUrl);

            Thread.sleep(1000);

            if (StrUtil.isBlank(searchResultUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }

            return searchResultUrl;

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"搜索失败");
        }

    }


    public static void main(String[] args) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl("https://img2.baidu.com/it/u=731696275,583862711&fm=253&fmt=auto&app=138&f=JPEG?w=800&h=1422");
        System.out.println("imagePageUrl = " + imagePageUrl);
    }
}
