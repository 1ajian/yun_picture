package com.jianzhao.picturebackend.api.so_360.sub;

import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * ClassName: GetImageUrlAPI
 * Package: com.jianzhao.picturebackend.api.so_360.sub
 * Description:
 *  请求页面获取imgUrl
 * @Author 阿小健
 * @Create 2025/5/26 14:00
 * @Version 1.0
 */
@Slf4j
public class GetImageUrlApi {
    public static String getImageUrl(String pictureUrl) {
        String url = "https://st.so.com/r?src=st&srcsp=home&img_url=" + pictureUrl + "&submittype=imgurl";
        log.info("url = {}", url);
        try {
            //获取响应文档
            Document document = Jsoup.connect(url).timeout(5000).get();
            Element element = document.selectFirst(".img_img");
            if (element != null) {
                String style = element.attr("style");
                if (style.contains("background-image:url(")) {
                    //提取出想要的url
                    int begin = style.indexOf("url(") + 4;
                    int end = style.indexOf(")",begin);
                    if (end > begin && begin > 4) {
                        return style.substring(begin, end);
                    }
                }
            }
        } catch (Exception e) {
            log.error("搜索图片失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索图片失败");
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR,"搜索图片失败");
    }

    public static void main(String[] args) {
        String imageUrl = GetImageUrlApi.getImageUrl("https://cloud-picture-1349091286.cos.ap-guangzhou.myqcloud.com/public/1926551303337172993/2025-05-26_4575dc88.webp");
        System.out.println("imageUrl = " + imageUrl);
    }
}
