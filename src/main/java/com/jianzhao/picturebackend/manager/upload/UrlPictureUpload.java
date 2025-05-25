package com.jianzhao.picturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.jianzhao.picturebackend.exception.ThrowUtils;
import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import com.jianzhao.picturebackend.model.enums.ImageFormatEnum;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * ClassName: UrlPictureUpload
 * Package: com.yupi.yupicturebackend.manager.upload
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/6 15:13
 * @Version 1.0
 */
@Component
public class UrlPictureUpload extends PictureUploadTemplate {
    /**
     * 获取本地临时文件
     * @param inputSource
     * @param file
     */
    @Override
    protected void getLocalTemplateFile(Object inputSource, File file) {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.getName(fileUrl);
    }

    @Override
    protected String checkInputSource(Object inputSource) {

        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR,"文件地址不能为空");
        String fileUrl = (String) inputSource;

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
        ImageFormatEnum imageFormatEnum = null;
        String suffix = "";

        try {
            //3.发送HEAD请求验证文件是否存在
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                //return;
                //不报错 仅对能获取到的信息进行校验。
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "请求文件不存在");
            }
            //校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                imageFormatEnum = ImageFormatEnum.checkMimeType(contentType);
                ThrowUtils.throwIf(ObjUtil.isNull(imageFormatEnum),ErrorCode.PARAMS_ERROR,"文件的类型不被允许");
            }
            //返回的后缀
            if (ObjUtil.isNotNull(imageFormatEnum)) {
                suffix = imageFormatEnum.getExtension();
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

        return suffix;
    }
}
