package com.jianzhao.picturebackend.manager.capture;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jianzhao.picturebackend.config.CapturePictureConfig;
import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: BatchCaptureImagesFromBaidu
 * Package: com.jianzhao.picturebackend.manager.capture
 * Description:
 *  通过百度批量抓取图片
 * @Author 阿小健
 * @Create 2025/5/24 12:28
 * @Version 1.0
 */
@Slf4j
@Component
public class BatchCaptureImagesFromBaidu {

    @Autowired
    private CapturePictureConfig capturePictureConfig;
    private String cookies = "";

    /**
     * 搜索百度图片并获取指定数量的图片URL
     * @param keyword 搜索关键词
     * @param count 需要获取的图片数量
     * @return 图片URL列表
     */
    public List<String> searchImages(String keyword, int count,int begin) {
        List<String> imageUrls = new ArrayList<>();
        int pn = begin; // 起始位置
        int rn = 30; // 每次请求获取的数量(最大30)

        try {
            // 首次获取cookies
            refreshCookies();

            while (imageUrls.size() < count) {
                String url = buildRequestUrl(keyword, pn, rn);
                //System.out.println(url);
                String result = sendRequest(url);

                if (StrUtil.isBlank(result)) {
                    log.error("获取数据失败，可能触发反爬");
                    break;
                }

                // 解析图片URL
                List<String> urls = parseImageUrls(result);
                imageUrls.addAll(urls);

                // 更新位置
                pn += rn;

                // 防止请求过于频繁
                TimeUnit.MILLISECONDS.sleep(1500 + RandomUtil.randomInt(500));

                // 如果已经获取足够数量，提前结束
                if (imageUrls.size() >= count) {
                    break;
                }
            }

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }

        // 返回指定数量的图片URL
        return imageUrls.size() > count ? imageUrls.subList(0, count) : imageUrls;
    }

    /**
     * 构建请求URL
     * @param keyword
     * @param pn
     * @param rn
     * @return
     */
    private String buildRequestUrl(String keyword, int pn, int rn) {
        return "https://image.baidu.com/search/acjson?" +
                "tn=resultjson_com" +
                "&ipn=rj" +
                "&ct=201326592" +
                "&is=" +
                "&fp=result" +
                "&cl=2" +
                "&lm=-1" +
                "&ie=utf-8" +
                "&oe=utf-8" +
                "&word=" + URLUtil.encode(keyword) +
                "&pn=" + pn +
                "&rn=" + rn +
                "&gsm=" + Integer.toHexString(pn) +
                "&callback=json_" + System.currentTimeMillis();
    }

    /**
     *  发送HTTP请求，获取JSON数据
     * @param url
     * @return
     * @throws InterruptedException
     */
    private String sendRequest(String url) {
        String jsonResponse = null;
        try {
            HttpResponse response = HttpRequest.get(url)
                    .header("Accept", "text/javascript, application/javascript, */*; q=0.01")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Referer", "https://image.baidu.com/")
                    .header("User-Agent", getRandomUserAgent())
                    .header("X-Requested-With", "XMLHttpRequest")
                    .cookie(cookies)
                    .timeout(15000)
                    .execute();

            // 处理重定向
            if (response.getStatus() == 302) {
                refreshCookies();
                return null;
            }

            jsonResponse = response.body();

            // 处理JSONP响应
            if (jsonResponse.startsWith("json_") && jsonResponse.contains("(") && jsonResponse.endsWith(")")) {
                int start = jsonResponse.indexOf("(") + 1;
                int end = jsonResponse.lastIndexOf(")");
                jsonResponse = jsonResponse.substring(start, end);
            }
        } catch (HttpException e) {
            log.error("图片请求失败:{}",e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }

        return jsonResponse;
    }

    /**
     * 解析图片URL
     * @param jsonStr
     * @return
     */
    private List<String> parseImageUrls(String jsonStr) {
        List<String> urls = new ArrayList<>();

        try {
            JSONObject json = JSONUtil.parseObj(jsonStr);
            JSONArray data = json.getJSONArray("data");

            if (CollectionUtil.isNotEmpty(data)) {
                for (int i = 0; i < data.size(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    String thumbUrl = item.getStr("thumbURL");
                    String middleUrl = item.getStr("middleURL");

                    // 优先使用thumbURL，如果没有则使用middleURL
                    String url = thumbUrl != null ? thumbUrl : middleUrl;
                    if (url != null && !url.isEmpty()) {
                        urls.add(url);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析JSON失败:{} ", e.getMessage());
        }

        return urls;
    }

    /**
     * 建立连接，更新 cookies
     */
    private void refreshCookies() {
        //先创建连接
        HttpResponse homeResponse = HttpRequest.get("https://image.baidu.com/")
                .header("User-Agent", getRandomUserAgent())
                .execute();

        // 获取Cookie
        String newCookies = homeResponse.header("Set-Cookie");
        if (newCookies != null) {
            cookies = newCookies;
        }
    }

    /**
     * 随机获取一个用户代理
     * @return
     */
    private String getRandomUserAgent() {
        return RandomUtil.randomEle(capturePictureConfig.getList());
    }

}
