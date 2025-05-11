package com.yupi.yupicturebackend.manager;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.enums.ImageFormatEnum;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ClassName: FileManager
 * Package: com.yupi.yupicturebackend.manager
 * Description:
 *      上传图片
 * @Author 阿小健
 * @Create 2025/4/25 15:26
 * @Version 1.0
 */
@Service
@Slf4j
@Deprecated
public class FileManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     * @param multipartFile 文件
     * @param uploadPathPrefix 上传文件的前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String uploadPathPrefix) {
        //校验图片
        validPicture(multipartFile);

        String datePrifix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //构建图片上传地址
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String uploadFileName = String.format("%s_%s.%s", datePrifix, uuid, suffix);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        //上传图片
        File file = null;

        try {
            //构建临时图片文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);

            //log.info("上传的key:{}",uploadPath);
            //上传并获取结构
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //数据万象解析结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            //获取宽高比
            int height = imageInfo.getHeight();
            int width = imageInfo.getWidth();
            Double picScale = width * 1.0 / height;
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
            uploadPictureResult.setPicName(uploadFileName);
            uploadPictureResult.setPicSize(multipartFile.getSize());
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            return uploadPictureResult;

        } catch (Exception e) {
            log.error("图片上传到对象存储服务失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteFile(file);
        }
    }


    /**
     * 校验文件
     * @param multipartFile 文件
     */
    public void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, new BusinessException(ErrorCode.PARAMS_ERROR,"上传文件为空"));
        //验证大小
        long size = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L;
        //TODO:有坑 因为servlet默认文件大小是1MB
        ThrowUtils.throwIf(size > 2 * ONE_M, new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M"));
        //验证后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //允许上传文件的后缀
        //final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg","jpg","png","webp");
        //ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), new BusinessException(ErrorCode.PARAMS_ERROR,"文件类型错误"));

        //优化：补充更严格的校验，比如为支持的图片格式定义枚举，仅允许上传枚举定义的格式。
        ImageFormatEnum imageFormatEnum = ImageFormatEnum.getImageFormatEnumBySuffix(fileSuffix);
        ThrowUtils.throwIf(imageFormatEnum == null, new BusinessException(ErrorCode.PARAMS_ERROR,"文件类型错误"));
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
     * 删除云上的图片
     * @param url
     */
    public void deletePicForCOS(String url) {
        String key = url.replace(cosClientConfig.getHost(), "");
        cosManager.deleteForCos(key);
    }

    /**
     * 通过url上传图片
     * @param fileUrl 图片url
     * @param uploadPathPrefix 上传的前缀
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl,String uploadPathPrefix) {
        //校验图片
        validPicture(fileUrl);

        String datePrifix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //构建图片上传地址
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
        String originalFilename = FileUtil.mainName(fileUrl);
        String suffix = FileUtil.getSuffix(originalFilename);
        String uploadFileName = String.format("%s_%s.%s", datePrifix, uuid, suffix);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        //上传图片
        File file = null;

        try {
            //构建临时图片文件
            file = File.createTempFile(uploadPath, null);
            //multipartFile.transferTo(file);
            //下载文件到本地临时存储
            HttpUtil.downloadFile(fileUrl, file);

            //log.info("上传的key:{}",uploadPath);
            //上传并获取结构
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //数据万象解析结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            //获取宽高比
            int height = imageInfo.getHeight();
            int width = imageInfo.getWidth();
            Double picScale = width * 1.0 / height;
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
            uploadPictureResult.setPicName(uploadFileName);
            uploadPictureResult.setPicSize(file.length());
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            return uploadPictureResult;

        } catch (Exception e) {
            log.error("图片上传到对象存储服务失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteFile(file);
        }
    }

    /**
     * 校验url图片
     * @param fileUrl
     */
    public void validPicture(String fileUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl),ErrorCode.PARAMS_ERROR,"文件地址不能为空");
        //1.验证url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件地址格式不正确");
        }
        //2.校验url协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR,"仅支持HTTP或HTTPS协议的文件地址");

        HttpResponse response = null;
        try {
            //3.发送HEAD请求验证文件是否存在
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
                //不报错 仅对能获取到的信息进行校验。
                //throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "请求文件不存在");
            }
            //校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                ImageFormatEnum imageFormatEnum = ImageFormatEnum.checkMimeType(contentType);
                ThrowUtils.throwIf(ObjUtil.isNull(imageFormatEnum),ErrorCode.PARAMS_ERROR,"文件的类型不被允许");
            }

            //校验文件大小
            String contentLengthStr = response.header("Content-Length");
            try {
                if (StrUtil.isNotBlank(contentLengthStr)) {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L;
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR,"文件大小不能超过2M");
                }
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件大小格式错误");
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
