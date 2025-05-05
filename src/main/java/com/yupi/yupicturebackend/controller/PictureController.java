package com.yupi.yupicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.org.apache.bcel.internal.generic.RETURN;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.*;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebackend.model.vo.PictureTagCategory;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * ClassName: PictureController
 * Package: com.yupi.yupicturebackend.controller
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/26 12:26
 * @Version 1.0
 */
@RestController
@RequestMapping("/picture")
//@Api(tags = "图片接口")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * 上传图片
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    //@ApiOperation(value = "上传图片")
    @PostMapping("/upload")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestBody MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    //@ApiOperation(value = "删除图片")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        //先判断删除请求对象符不符合要求
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空或者参数不正确");
        }

        //判断要删除的图片存不存在
        Long id = deleteRequest.getId();
        Picture picture = pictureService.getById(id);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        //判断当前用户的身份,只有本人或者管理员才能进行删除
        Long masterId = picture.getUserId();
        User loginUser = userService.getLoginUser(request);
        if (!loginUser.getId().equals(masterId) || !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result, new BusinessException(ErrorCode.OPERATION_ERROR));
        pictureService.deleteForCos(picture.getUrl());
        return ResultUtils.success(result);
    }

    /**
     * 更新图片（仅管理员可用）
     * @param pictureUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    //@ApiOperation(value = "更新图片（仅管理员可用）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,HttpServletRequest request) {
        //首先先判空和id值验证
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断是否存在
        Picture oldPicture = pictureService.getById(pictureUpdateRequest.getId());

        if (oldPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        //将dto转换为实体类
        //将tags List类型转换为string
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

        //数据校验
        pictureService.validPicture(picture);

        //获取登录用户
        User loginUser = userService.getLoginUser(request);

        //补充审核参数（代码复用）
        pictureService.fillReviewParams(picture,loginUser);

        //操作数据库
        boolean res = pictureService.updateById(picture);
        ThrowUtils.throwIf(!res,new BusinessException(ErrorCode.OPERATION_ERROR, "修改图片失败"));

        return ResultUtils.success(res);
    }

    /**
     * 根据id获取图片信息（仅管理员可用）
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    //@ApiOperation(value = "根据id获取图片信息（仅管理员可用）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        return ResultUtils.success(picture);
    }

    /**
     * 根据Id获取图片(封装类)
     * @param id
     * @return
     */
    //@ApiOperation(value = "根据Id获取图片(封装类)")
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        //TODO：优化,如果图片的审核状态是未审核，则不能访问 可能需要加上管理员！
        if (PictureReviewStatusEnum.REJECT.getValue().equals(picture.getReviewStatus())) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR,"获取图片的审核状态为未审核");
        }

        return ResultUtils.success(PictureVO.objToVo(picture));
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    //@ApiOperation(value = "分页获取图片列表（仅管理员可用）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long pageSize = pictureQueryRequest.getPageSize();
        long current = pictureQueryRequest.getCurrent();

        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(page);
    }

    /**
     * 分页获取图片列表（封装类）
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    //@ApiOperation(value = "分页获取图片列表（封装类）")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(pageSize > 20, new BusinessException(ErrorCode.PARAMS_ERROR));
        //设置能看到的只有审核通过的
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //分页查询pictureVO
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(page, request);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑图片(给用户使用)
     * @param pictureEditRequest
     * @param request
     * @return
     */
    //@ApiOperation(value = "编辑图片(给用户使用)")
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        //参数校验
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //实体类和DTO进行转换
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, picture);
        //将tags 的List转为string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));

        //数据校验
        pictureService.validPicture(picture);
        //设置编辑时间
        picture.setEditTime(new Date());

        //判断要修改的图片是否存在
        Long pictureId = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(pictureId);
        if (oldPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        //仅本人和管理员可编辑
        //获取当前用户信息
        User loginUser = userService.getLoginUser(request);
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //给图片填充审核参数
        pictureService.fillReviewParams(picture, loginUser);

        //操作数据库
        boolean res = pictureService.updateById(picture);
        ThrowUtils.throwIf(!res, new BusinessException(ErrorCode.OPERATION_ERROR));
        return ResultUtils.success(res);
    }

    /**
     * 获取标签_分类信息
     * @return
     */
    @GetMapping("/tag_category")
    //@ApiOperation(value = "获取标签_分类信息")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {

        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 图片审核（管理员）
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    //@ApiOperation(value = "图片审核")
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }
}