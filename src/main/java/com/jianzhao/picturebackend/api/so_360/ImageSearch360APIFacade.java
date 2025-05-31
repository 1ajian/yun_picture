package com.jianzhao.picturebackend.api.so_360;

import com.jianzhao.picturebackend.api.so_360.model.ImageSearch360Result;
import com.jianzhao.picturebackend.api.so_360.sub.GetImageListApi;
import com.jianzhao.picturebackend.api.so_360.sub.GetImageUrlApi;

import java.util.List;

/**
 * ClassName: So360ImageSearchResult
 * Package: com.jianzhao.picturebackend.api.so_360.model
 * Description:
 *  门面模式 统一接口 360图片搜索
 * @Author 阿小健
 * @Create 2025/5/26 11:27
 * @Version 1.0
 */
public class ImageSearch360APIFacade {
    /**
     * 结合所有360图片搜索接口
     * @param imageUrl
     * @param start
     * @return
     */
    public static List<ImageSearch360Result> searchImage(String imageUrl, Integer start) {
        String url = GetImageUrlApi.getImageUrl(imageUrl);
        List<ImageSearch360Result> result = GetImageListApi.getImageList(start,url);
        return result;
    }

    public static void main(String[] args) {
        List<ImageSearch360Result> result = ImageSearch360APIFacade.searchImage("https://cloud-picture-1349091286.cos.ap-guangzhou.myqcloud.com/public/1926551303337172993/2025-05-26_4575dc88.webp",20);
        System.out.println(result);
    }
}
