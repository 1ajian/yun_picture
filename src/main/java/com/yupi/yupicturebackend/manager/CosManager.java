package com.yupi.yupicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.model.entity.Picture;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ClassName: CosManager
 * Package: com.yupi.yupicturebackend.manager
 * Description:
 *  COS上传对象
 * @Author 阿小健
 * @Create 2025/4/25 13:39
 * @Version 1.0
 */
@Component
public class CosManager {
    @Resource
    private COSClient cosClient;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传对象
     * @param key 上传的唯一键 例如"abc/abc.txt"
     * @param file 文件 例如= "abc.txt"
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }


    /**
     * 下载对象
     * @param key 文件路径
     * @return
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }


    /**
     * 上传图片对象（附带图片信息） 并 数据万象处理
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key,File file) {
        //上传请求参数
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        //对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        //表示返回原图信息
        picOperations.setIsPicInfo(1);

        List<PicOperations.Rule> rules = new ArrayList<>();
        //图片压缩为（转为webp格式）
        String webp = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule rule = new PicOperations.Rule();
        rule.setBucket(cosClientConfig.getBucket());
        rule.setFileId(webp);
        rule.setRule("imageMogr2/format/webp");
        rules.add(rule);

        //缩略图处理，对20KB的图片生成缩略图
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 1024,1024));
            rules.add(thumbnailRule);
        }

        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        //存储对象
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 直接用流的方式将请求中的文件上传到 COS(没有用上)
     * @param multipartFile
     * @param key
     * @return 图片的访问路径
     */
    public PutObjectResult uploadToCOS(MultipartFile multipartFile,String key) throws Exception {
        InputStream inputStream = null;
        try {
            inputStream = multipartFile.getInputStream();
            //元信息配置
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(multipartFile.getSize());
            metadata.setContentType(multipartFile.getContentType());

            PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, inputStream, metadata);

            return cosClient.putObject(putObjectRequest);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传到COS失败");
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

    }

    /**
     * 从云上删除图片对象
     * @param url
     */
    public void deleteForCos(String url) {
        try {
            if (StrUtil.isNotBlank(url)) {
                String key = url.replace(cosClientConfig.getHost(), "");
                cosClient.deleteObject(cosClientConfig.getBucket(),key);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"腾讯云上对象删除失败");
        }
    }


    /**
     * 云上批量删除图片
     * @param pictures
     */
    public void deleteBatchPicture(List<Picture> pictures) {
        List<String> urls = pictures.stream().map(picture -> picture.getUrl().replace(cosClientConfig.getHost() + "/", "")).collect(Collectors.toList());
        List<String> thumbnailUrls = pictures.stream().map(picture -> picture.getThumbnailUrl().replace(cosClientConfig.getHost() + "/", "")).collect(Collectors.toList());
        List<String> originalUrls = pictures.stream().map(picture -> picture.getOriginalUrl().replace(cosClientConfig.getHost() + "/", "")).collect(Collectors.toList());
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(cosClientConfig.getBucket());

        //设置要删除Key的列表
        ArrayList<DeleteObjectsRequest.KeyVersion> keyList = new ArrayList<>();
        for (String url : urls) {
            keyList.add(new DeleteObjectsRequest.KeyVersion(url));
        }

        for (String thumbnailUrl : thumbnailUrls) {
            keyList.add(new DeleteObjectsRequest.KeyVersion(thumbnailUrl));
        }

        for (String originalUrl : originalUrls) {
            keyList.add(new DeleteObjectsRequest.KeyVersion(originalUrl));
        }

        deleteObjectsRequest.setKeys(keyList);

        try {
            cosClient.deleteObjects(deleteObjectsRequest);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
        } finally {
            cosClient.shutdown();
        }
    }
}
