package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.model.dto.picture.*;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 86135
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-04-25 14:58:06
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     * @param inputSource 媒体文件
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser 登录用户
     * @return
     */

    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取图片查询的queryWrapper对象
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

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
     *  清理图片
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

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

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                 User loginUser);

    /**
     * 批量删除图片
     * @param pictureIds
     */
    void deleteBatchPicture(List<Long> pictureIds);


    /**
     * 校验权限 登录用户必须是空间创建人
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    Boolean deletePicture(DeleteRequest deleteRequest, User loginUser);

    /**
     * 编辑图片
     * @param pictureEditRequest
     * @param loginUser
     * @return
     */
    Boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 颜色搜索
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 修改我的空间内图片的主色调
     */
    void updateAllAve(Long spaceId);

    /**
     * 批量编辑图片
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建图片扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
