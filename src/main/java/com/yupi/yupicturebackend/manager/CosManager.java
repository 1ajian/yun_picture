package com.yupi.yupicturebackend.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
     * @param key
     */
    public void deleteForCos(String key) {
        try {
            cosClient.deleteObject(cosClientConfig.getBucket(),key);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"腾讯云上对象删除失败");
        }
    }
}
