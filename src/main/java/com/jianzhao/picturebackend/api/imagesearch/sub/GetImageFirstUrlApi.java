package com.jianzhao.picturebackend.api.imagesearch.sub;

import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ClassName: GetImageFirstUrlApi
 * Package: com.yupi.yupicturebackend.api.imagesearch.sub
 * Description:
 *  获取图片列表页面地址
 * @Author 阿小健
 * @Create 2025/5/13 9:51
 * @Version 1.0
 */
@Slf4j
@SuppressWarnings("all")
public class GetImageFirstUrlApi {
    /**
     * 获取图片列表页面地址
     * @param url
     * @return
     */
    public static String getImageFirstUrl(String url) {
        try {
            Document document = Jsoup.connect(url).timeout(5000).get();
            Elements scriptElements = document.getElementsByTag("script");
            for (Element script : scriptElements) {
                String scriptContent = script.html();
                if (scriptContent.contains("\"firstUrl\"")) {
                    //使用正则表达式提取 firstUrl 的值
                    Pattern pattern = Pattern.compile("\"firstUrl\"\\s*:\\s*\"(.*?)\"");
                    Matcher matcher = pattern.matcher(scriptContent);

                    if (matcher.find()) {
                        String firstUrl = matcher.group(1);
                        firstUrl = firstUrl.replace("\\/", "/");
                        return firstUrl;
                    }

                }
            }
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"未找到url");
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"搜索失败");
        }
    }

    public static void main(String[] args) {
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl("https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData[isLogoShow]=1&f=all&isLogoShow=1&session_id=14079056900383585531&sign=126584d2788b75492145301747104945&tpl_from=pc");
    }
}
