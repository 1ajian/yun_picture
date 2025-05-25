package com.jianzhao.picturebackend.test;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.*;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BaiduImageSearcher {

    private static String cookies = "";

    /**
     * 搜索百度图片并获取指定数量的图片URL
     * @param keyword 搜索关键词
     * @param count 需要获取的图片数量
     * @return 图片URL列表
     */
    public static List<String> searchImages(String keyword, int count) {
        List<String> imageUrls = new ArrayList<>();
        int pn = 100; // 起始位置
        int rn = 30; // 每次请求获取的数量(最大30)

        try {
            // 首次获取cookies
            refreshCookies();

            while (imageUrls.size() < count) {
                String url = buildRequestUrl(keyword, pn, rn);
                System.out.println(url);
                String result = sendRequest(url);

                if (StrUtil.isBlank(result)) {
                    System.out.println("获取数据失败，可能触发反爬");
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
            e.printStackTrace();
        }

        // 返回指定数量的图片URL
        return imageUrls.size() > count ? imageUrls.subList(0, count) : imageUrls;
    }

    private static String buildRequestUrl(String keyword, int pn, int rn) {
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

    private static String sendRequest(String url) throws InterruptedException {
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

        String jsonResponse = response.body();

        // 处理JSONP响应
        if (jsonResponse.startsWith("json_") && jsonResponse.contains("(") && jsonResponse.endsWith(")")) {
            int start = jsonResponse.indexOf("(") + 1;
            int end = jsonResponse.lastIndexOf(")");
            jsonResponse = jsonResponse.substring(start, end);
        }

        return jsonResponse;
    }

    private static List<String> parseImageUrls(String jsonStr) {
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
            System.out.println("解析JSON失败: " + e.getMessage());
        }

        return urls;
    }

    private static void refreshCookies() {
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

    private static String getRandomUserAgent() {
        return RandomUtil.randomEle(UserAgents.USER_AGENTS);
    }

    private static class UserAgents {
        static final String[] USER_AGENTS = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0"
        };
    }

    public static void main(String[] args) {
        // 示例：搜索"风景"并获取50张图片
        List<String> images = searchImages("美杜莎女王", 10);

        System.out.println("获取到图片数量: " + images.size());
        System.out.println("前5张图片URL:");
        for (int i = 0; i < Math.min(10, images.size()); i++) {
            System.out.println((i+1) + ". " + images.get(i));
        }
    }
}