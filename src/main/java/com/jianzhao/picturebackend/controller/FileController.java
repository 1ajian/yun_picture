package com.jianzhao.picturebackend.controller;

import com.jianzhao.picturebackend.constant.UserConstant;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.utils.IOUtils;
import com.jianzhao.picturebackend.annotation.AuthCheck;
import com.jianzhao.picturebackend.common.BaseResponse;
import com.jianzhao.picturebackend.common.ResultUtils;
import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import com.jianzhao.picturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * ClassName: FileController
 * Package: com.yupi.yupicturebackend.controller
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/25 13:51
 * @Version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        //获取原文件名称
        String filename = multipartFile.getOriginalFilename();
        //定义在COS中的存储路径（虚拟路径，非本地路径）
        String filePath = String.format("/test/%s", filename);

        File file = null;
        PutObjectResult putObjectResult = null;
        try {
            // 生成在系统临时目录
            file = File.createTempFile(filePath,null);
            //将上传文件写入临时文件
            multipartFile.transferTo(file);
            //调用上传的方法
            putObjectResult = cosManager.putObject(filePath, file);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filePath);
                }
            }
        }
        return ResultUtils.success(filePath);
    }


    /**
     * 测试下载
     * @param filePath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filePath, HttpServletResponse response) {
        //根据文件路径获取cos对象
        COSObject cosObject = cosManager.getObject(filePath);
        //获得输入流对象
        COSObjectInputStream cosObjectInputStream = cosObject.getObjectContent();

        try {
            //输入流转换为数组
            byte[] bytes = IOUtils.toByteArray(cosObjectInputStream);
            //设置响应头
            //是流
            response.setContentType("application/octet-stream;charset=UTF-8");
            //要下载
            response.setHeader("Content-Disposition", "attachment; filename=" + filePath);

            //写到输出流
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            try {
                if (cosObjectInputStream != null) {
                    cosObjectInputStream.close();
                }
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"流关闭错误");
            }
        }

    }
}
