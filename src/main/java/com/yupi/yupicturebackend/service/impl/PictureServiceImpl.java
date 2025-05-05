package com.yupi.yupicturebackend.service.impl;
import java.util.List;
import java.util.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.FileManager;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureReviewRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
* @author 86135
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-04-25 14:58:06
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Autowired
    private UserService userService;
    @Autowired
    private FileManager fileManager;

    /**
     * 上传图片
     * @param multipartFile 媒体文件
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser 登录用户
     * @return
     */

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
        //判断是新增还是更新图片
        Long pictureId = null;

        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }

        //如果是更新图片需要判断图片是否存在 并且图片上传功能是支持图片编辑的，所以要做好编辑权限控制(本人或者管理员)
        if (pictureId != null) {
            /*boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId).exists();
            ThrowUtils.throwIf(!exists, new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));*/

            //获取图片信息
            Picture picture = this.getById(pictureId);
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR,"图片不存在");
            //权限控制(本人或者管理员)
            if (!userService.isAdmin(loginUser) && !picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        //上传图片，得到信息
        //按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        //构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());

        //如果pictureId不为空，表示更新，是否新增
        if (pictureId != null) {
            //如果是更新，需要补充id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        //填充审核参数
        this.fillReviewParams(picture, loginUser);

        //保存数据库并返回VO对象
        boolean saveRes = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!saveRes,ErrorCode.OPERATION_ERROR,"图片上传失败");
        PictureVO pictureVO = PictureVO.objToVo(picture);
        pictureVO.setUser(userService.getUserVO(loginUser));
        return pictureVO;
    }

    /**
     * 获取图片查询的queryWrapper对象
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public LambdaQueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        //判空
        if (pictureQueryRequest == null) {
            return null;
        }
        //构建返回结果
        LambdaQueryWrapper<Picture> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();

        //从多个字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            //需要拼接查询条件
            lambdaQueryWrapper.and(qw -> qw.like(Picture::getName, searchText).or().like(Picture::getIntroduction, searchText));
        }

        lambdaQueryWrapper.eq(id != null,Picture::getId, id)
                .eq(userId != null,Picture::getUserId, userId)
                .like(StrUtil.isNotBlank(name), Picture::getName,name)
                .like(StrUtil.isNotBlank(introduction), Picture::getIntroduction,introduction)
                .eq(StrUtil.isNotBlank(category), Picture::getCategory,category)
                .eq(picSize != null, Picture::getPicSize,picSize)
                .eq(ObjUtil.isNotEmpty(picWidth), Picture::getPicWidth, picWidth)
                .eq(ObjUtil.isNotEmpty(picHeight), Picture::getPicHeight, picHeight)
                .eq(ObjUtil.isNotEmpty(picScale), Picture::getPicScale,picScale)
                .eq(ObjUtil.isNotEmpty(reviewStatus),Picture::getReviewStatus,reviewStatus)
                .like(StrUtil.isNotBlank(reviewMessage), Picture::getReviewMessage,reviewMessage)
                .eq(ObjUtil.isNotEmpty(reviewerId), Picture::getReviewerId,reviewerId)
                .like(ObjUtil.isNotEmpty(picFormat), Picture::getPicFormat,picFormat)
                .orderBy(StrUtil.isNotEmpty(sortField),"ascend".equals(sortOrder), picture -> sortField);

        if (!CollectionUtils.isEmpty(tags)) {
            for (String tag : tags) {
                lambdaQueryWrapper.like(Picture::getTags,"\"" + tag + "\"");
            }
        }
        return lambdaQueryWrapper;
    }

    /**
     * 编写获取图片封装的方法，可以为原有的图片关联创建用户的信息
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        PictureVO pictureVO = PictureVO.objToVo(picture);
        User loginUser = userService.getLoginUser(request);
        UserVO userVO = userService.getUserVO(loginUser);
        pictureVO.setUser(userVO);
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        //获取分页之后图片列表
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(),picturePage.getSize(),picturePage.getTotal());
        if (CollectionUtils.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        //全部转换为VO
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        //用户ids
        Set<Long> userIds = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        //userId -> user
        Map<Long, List<User>> map = userService.listByIds(userIds).stream().collect(Collectors.groupingBy(User::getId));
        //填充信息
        for (PictureVO pictureVO : pictureVOList) {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (map.containsKey(userId)) {
                user = map.get(userId).get(0);
            }
            //给每个Vo对象设置对应的user对象
            pictureVO.setUser(userService.getUserVO(user));
        }

        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 编写图片数据校验方法，用于更新和修改图片时进行判断
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空"));

        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        ThrowUtils.throwIf(id == null, new BusinessException(ErrorCode.PARAMS_ERROR,"图片id为空"));
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 256,ErrorCode.PARAMS_ERROR,"图片的长度过长");
        }

        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 1024, new BusinessException(ErrorCode.PARAMS_ERROR, "简介过长"));
        }

    }

    /**
     * 删除云上的照片
     * @param url
     */
    @Override
    public void deleteForCos(String url) {
        fileManager.deletePicForCOS(url);
    }

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //首先通过参数判断
        Long pictureId = pictureReviewRequest.getId();
        Integer pictureReviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(pictureReviewStatus);

        if (pictureId == null || pictureReviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(pictureReviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //判断图片是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR);
        //判断图片状态是否和参数中一致
        if (pictureReviewStatus.equals(oldPicture.getReviewStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请勿重复审核");
        }

        //更新审核状态
        Picture picture = new Picture();
        BeanUtil.copyProperties(oldPicture, picture);
        picture.setReviewTime(new Date());
        picture.setReviewerId(loginUser.getId());
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 补充审核参数（公共方法）
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture,User loginUser) {
        if (userService.isAdmin(loginUser)) {
            //是管理员,自动过审
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
        }else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }
}




