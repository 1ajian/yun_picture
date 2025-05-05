package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureReviewRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author 86135
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-04-25 14:58:06
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     * @param multipartFile 媒体文件
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser 登录用户
     * @return
     */

    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取图片查询的queryWrapper对象
     * @param pictureQueryRequest
     * @return
     */
    LambdaQueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 编写获取图片封装的方法，可以为原有的图片关联创建用户的信息
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 编写图片数据校验方法，用于更新和修改图片时进行判断
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 删除云上的照片
     * @param url
     */
    void deleteForCos(String url);

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest,User loginUser);

    /**
     * 补充审核参数（公共方法）
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture,User loginUser);
}
