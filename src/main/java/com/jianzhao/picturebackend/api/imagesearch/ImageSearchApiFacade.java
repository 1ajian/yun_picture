package com.jianzhao.picturebackend.api.imagesearch;

import com.jianzhao.picturebackend.api.imagesearch.model.ImageSearchResult;
import com.jianzhao.picturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.jianzhao.picturebackend.api.imagesearch.sub.GetImageListApi;
import com.jianzhao.picturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * ClassName: ImageSearchApiFacade
 * Package: com.yupi.yupicturebackend.api.imagesearch
 * Description:
 *  门面模式：提供一个统一的接口来简化多个接口的调用 以图搜图统一接口调用
 * @Author 阿小健
 * @Create 2025/5/13 11:31
 * @Version 1.0
 */
@Slf4j
public class ImageSearchApiFacade {
    /**
     * 搜索图片
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> result = GetImageListApi.getImageList(imageFirstUrl);
        return result;
    }

    public static void main(String[] args) {
        List<ImageSearchResult> imageSearchResults = searchImage("https://cloud-picture-1349091286.cos.ap-guangzhou.myqcloud.com/public/1915029936313888769/2025-05-13_3550b42f.jpeg");
        System.out.println(imageSearchResults);
    }
}
