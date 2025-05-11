package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.enums.ImageFormatEnum;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * ClassName: FilePictureUpload
 * Package: com.yupi.yupicturebackend.manager.upload
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/6 15:02
 * @Version 1.0
 */
@Component
public class FilePictureUpload extends PictureUploadTemplate {
    /**
     * 获取本地临时图片
     * @param inputSource
     * @param file
     */
    @Override
    protected void getLocalTemplateFile(Object inputSource, File file) {
        try {
            MultipartFile multipartFile = (MultipartFile) inputSource;
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传文件失败");
        }
    }

    /**
     * 获取原始文件名
     * @param inputSource
     * @return
     */
    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    /**
     * 检查参数
     * @param inputSource
     */
    @Override
    protected String checkInputSource(Object inputSource) {
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR,"文件不能为空");
        MultipartFile multipartFile = (MultipartFile) inputSource;
        //验证大小
        long size = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L;
        ThrowUtils.throwIf(size > 2 * ONE_M, new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M"));

        String originalFilename = multipartFile.getOriginalFilename();
        String fileSuffix = extractFileSuffix(originalFilename);
        ImageFormatEnum imageFormatEnum = ImageFormatEnum.getImageFormatEnumBySuffix(fileSuffix);
        ThrowUtils.throwIf(imageFormatEnum == null, new BusinessException(ErrorCode.PARAMS_ERROR,"文件类型错误"));

        return fileSuffix;
    }
}
