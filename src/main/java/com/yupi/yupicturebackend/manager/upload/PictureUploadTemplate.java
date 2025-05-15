package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.image.ImageLabelResponse;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.enums.ImageFormatEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ClassName: PictureUploadTemplate
 * Package: com.yupi.yupicturebackend.manager.upload
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/6 14:24
 * @Version 1.0
 */
@Component
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 抽象方法：获取本地临时文件
     * @param inputSource
     * @param file
     */
    protected abstract void getLocalTemplateFile(Object inputSource, File file);

    /**
     * 抽象方法：获取原始文件名
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 抽象方法：检查输入源
     * @param inputSource
     */
    protected abstract String checkInputSource(Object inputSource);

    /**
     * 上传图片接口
     * @param inputSource url 或者是 multipartFile
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        //校验图片
        //TODO:抽象方法 检查输入源
        String fileSuffix = checkInputSource(inputSource);

        //获取获取 FileName
        String datePrifix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //构建图片上传地址
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
        //TODO：抽象方法 获取原始的文件名
        //String originalFilename = getOriginalFilename(inputSource);
        //String suffix = FileUtil.getSuffix(originalFilename);

        String uploadFileName = String.format("%s_%s.%s", datePrifix, uuid, fileSuffix);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        //上传图片
        File file = null;

        try {
            //构建临时图片文件
            file = File.createTempFile(uploadPath, null);

            //获取本地临时文件
            getLocalTemplateFile(inputSource,file);

            //上传图片到对象存储服务
            return getUploadPictureResult(uploadFileName, uploadPath, file);

        } catch (Exception e) {
            log.error("图片上传到对象存储服务失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteFile(file);
        }
    }

    private UploadPictureResult getUploadPictureResult(String uploadFileName, String uploadPath, File file) {
        //上传并获取结构
        PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
        //数据万象解析结果
        ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

        //封装返回结果 （原图）
        //获取宽高比
        int height = imageInfo.getHeight();
        int width = imageInfo.getWidth();
        Double picScale = width * 1.0 / height;
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
        uploadPictureResult.setOriginalUrl(cosClientConfig.getHost() + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(uploadFileName));
        uploadPictureResult.setPicSize(file.length());

        //获取生成的标签结果集对象
        ImageLabelResponse imageLabelResponse = cosManager.getPictureTags(uploadPath);
        uploadPictureResult.setPictureTagsResponse(imageLabelResponse);

        /*uploadPictureResult.setPicWidth(width);
        uploadPictureResult.setPicHeight(height);
        uploadPictureResult.setPicScale(picScale);*/
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        //设置主色调
        uploadPictureResult.setPicColor(cosManager.getImageAve(uploadPath));
        //解析(压缩图)
        ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
        List<CIObject> objectList = processResults.getObjectList();

        //设置压缩图属性
        if (CollUtil.isNotEmpty(objectList)) {
            //压缩图的结果
            CIObject compressCiObject = objectList.get(0);

            height = compressCiObject.getHeight();
            width = compressCiObject.getWidth();
            picScale = width * 1.0 / height;
            //将之前的原图替换为缩略图
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressCiObject.getKey());
            uploadPictureResult.setPicSize(compressCiObject.getSize().longValue());
            uploadPictureResult.setPicFormat(compressCiObject.getFormat());

            //缩略图的结果
            CIObject thumbnailCiObject = compressCiObject;
            if (objectList.size() > 1) {
                thumbnailCiObject = objectList.get(1);
            }
            uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        }

        uploadPictureResult.setPicWidth(width);
        uploadPictureResult.setPicHeight(height);
        uploadPictureResult.setPicScale(picScale);

        return uploadPictureResult;
    }


    /**
     * 删除文件
     * @param file
     */
    public void deleteFile(File file) {
        if (file == null) {
            return;
        }

        boolean delete = file.delete();
        if (!delete) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }

    /**
     * 获取文件后缀，默认转成小写进行判断
     *
     * @param fileName 文件名
     * @return 文件后缀
     */
    protected String extractFileSuffix(String fileName) {
        return Optional.of(fileName)
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                .map(String::toLowerCase)
                .orElse("");
    }

}
