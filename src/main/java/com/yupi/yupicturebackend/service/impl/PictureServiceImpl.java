package com.yupi.yupicturebackend.service.impl;
import com.yupi.yupicturebackend.api.aliyunai.AliYunAiApi;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest.Parameters;
import java.awt.*;
import java.util.List;
import java.util.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.model.ciModel.image.ImageLabelResponse;
import com.qcloud.cos.model.ciModel.image.Label;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.config.ThreadPoolConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.manager.upload.FilePictureUpload;
import com.yupi.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.yupi.yupicturebackend.manager.upload.UrlPictureUpload;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.*;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.service.UserService;
import com.yupi.yupicturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
* @author 86135
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-04-25 14:58:06
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Autowired
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private AliYunAiApi aliYunAiApi;


    /**
     * 上传图片
     * @param inputSource url或者是图片文件
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser 登录用户
     * @return
     */

    @Override
    @Transactional
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, new BusinessException(ErrorCode.NOT_LOGIN_ERROR));

        //校验空间参数是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            //存在
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            //必须是空间创建人才能上传
//            if (!loginUser.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间操作权限");
//            }

            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间的条数不足");
            }

            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量不足");
            }

        }


        //TODO:这里可以使用工厂方法优化
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        //验证参数
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
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

            //更新图片（删除云上的旧图片） TODO:需要判断旧数据的url和发送的参数的url是否相等，如果不相等则需要删除，需要先知道前端传了哪一个url
            this.clearPictureFile(picture);

            //权限控制(本人或者管理员)
//            if (!userService.isAdmin(loginUser) && !picture.getUserId().equals(loginUser.getId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }

            //校验空间是否一致
            Long pictureSpaceId = picture.getSpaceId();
            //如果没有传递spaceId则复用原先的spaceId
            if (spaceId == null) {
                spaceId = pictureSpaceId;
            } else {
                //如果传递了spaceId，必须和原有图片一致（排除掉改为null的情况,让图片可以公开）
                if (!pictureSpaceId.equals(spaceId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                } else {
                    //恢复空间图片大小和数量
                    recoverSpaceCountAndSize(picture);
                }
            }

        }

        //上传图片，得到信息
        String uploadPathPrefix = null;
        if (spaceId == null) {
            //按照用户id划分目录
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            //根据空间划分目录
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        //构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setSpaceId(spaceId);
        picture.setOriginalUrl(uploadPictureResult.getOriginalUrl());
        picture.setPicColor(uploadPictureResult.getPicColor());

        //解析标签并赋值给图片
        ImageLabelResponse response = uploadPictureResult.getPictureTagsResponse();
        List<String> names = response.getRecognitionResult().stream().map(Label::getName).limit(3).collect(Collectors.toList());
        String tagsJson = JSONUtil.toJsonStr(names);
        picture.setTags(tagsJson);

        //有可能我修改的是图片的名称，这个名称不一定是上传之后生成的名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);

        //如果pictureId不为空，表示更新，是否新增
        if (pictureId != null) {
            //如果是更新，需要补充id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        // 补充空间 id，默认为 0（分表）
//        if (spaceId == null) {
//            picture.setSpaceId(0L);
//        }

        //填充审核参数
        this.fillReviewParams(picture, loginUser);

        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            //保存数据库并返回VO对象
            boolean saveRes = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!saveRes,ErrorCode.OPERATION_ERROR,"图片上传失败");
            //需要处理总数和大小
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);
            }
            return picture;
        });

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
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        //判空
        if (pictureQueryRequest == null) {
            return null;
        }

        //构建返回结果
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

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
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        queryWrapper.eq(id != null,"id", id)
                .eq(userId != null,"userId", userId)
                .like(StrUtil.isNotBlank(name), "name",name)
                .like(StrUtil.isNotBlank(introduction), "introduction",introduction)
                .eq(StrUtil.isNotBlank(category), "category",category)
                .eq(picSize != null, "picSize",picSize)
                .eq(ObjUtil.isNotNull(picWidth), "picWidth", picWidth)
                .eq(ObjUtil.isNotNull(picHeight), "picHeight", picHeight)
                .eq(ObjUtil.isNotNull(picScale), "picScale",picScale)
                .eq(ObjUtil.isNotNull(reviewStatus),"reviewStatus",reviewStatus)
                .like(StrUtil.isNotBlank(reviewMessage),"reviewMessage",reviewMessage)
                .eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId",reviewerId)
                .like(ObjUtil.isNotEmpty(picFormat), "picFormat",picFormat)
                .eq(ObjUtil.isNotEmpty(spaceId), "spaceId",spaceId)
                .isNull(nullSpaceId,"spaceId")
                .gt(ObjUtil.isNotNull(startEditTime), "editTime",startEditTime) // >=
                .lt(ObjUtil.isNotNull(endEditTime), "editTime",endEditTime) // <
                .orderBy(StrUtil.isNotBlank(sortField),"ascend".equals(sortOrder),sortField);

        //分表使用 并且需要去掉spaceId作为条件（当spaceId为0的时候）
        //queryWrapper.eq(nullSpaceId, "spaceId",0L);

        //从多个字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            //需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
        }

        if (!CollectionUtils.isEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags","\"" + tag + "\"");
            }
        }
        return queryWrapper;
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
     * 清理图片
     * @param oldPicture
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        String pictureUrl = oldPicture.getUrl();
        String originalUrl = oldPicture.getOriginalUrl();
        //可以避免 秒传的场景下，就有可能多个图片地址指向同一个文件。
        Long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl).count();
        if (count > 1) {
            return;
        }
        //清理原图
        cosManager.deleteForCos(originalUrl);

        //清理压缩图片
        cosManager.deleteForCos(pictureUrl);
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)){
            //清理缩略图
            cosManager.deleteForCos(thumbnailUrl);
        }

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

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //1.格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30,ErrorCode.PARAMS_ERROR,"最多30张图片");

        //对图片名前缀做判断并处理
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = pictureUploadByBatchRequest.getSearchText();
        }

        //2.拼接要抓取的地址(使用bing抓)
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", pictureUploadByBatchRequest.getSearchText());

        //3.使用jsonp
        Document document;
        try {
            //优化防止被禁
            Thread.sleep(300);
            document = Jsoup.connect(fetchUrl).get();
        } catch (Exception e) {
            log.error("获取页面失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"页面获取失败");
        }
        //根据 选择器获取标签
        Element div = document.getElementsByClass("dgControl").get(0);
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }

        //Elements imgElements = div.select("img.mimg");
        Elements imgElements = div.select("a.iusc");

        //上传的图片数量
        int uploadCount = 0;

        //根据 标签的属性获取url
        for (Element imgElement : imgElements) {
            //此处获取非原图
            //String fileUrl = imgElement.attr("src");
            /*if (StrUtil.isBlank(fileUrl)) {
                log.info("当前图链接为空,已跳过：{}",fileUrl);
                continue;
            }
            //处理图片上传地址，防止出现转义问题(去除?后面的参数)
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0,questionMarkIndex);
            }*/

            //优化获取原图
            String dataM = imgElement.attr("m");
            String fileUrl = null;
            try {
                JSONObject jsonObject = JSONUtil.parseObj(dataM);
                //获取murl字段(原图片url)
                fileUrl = jsonObject.getStr("murl");
            } catch (Exception e) {
                log.info("图片解析失败:url:{}",fileUrl);
                continue;

            }
            //结果判空
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前图片的url为空,已跳过:{}",fileUrl);
                continue;

            }

            //构建上传图片所需参数
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();

            //对上传对象设置图片名前缀
            if (StrUtil.isNotBlank(namePrefix)) {
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }

            //上传图片
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                uploadCount++;
                log.info("图片上传成功,id = {}",pictureVO.getId());

            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }

            if (uploadCount >= count) {
                break;
            }
        }

        return uploadCount;
    }

    /**
     * 批量删除图片
     * @param pictureIds
     */
    @Override
    public void deleteBatchPicture(List<Long> pictureIds) {
        List<Picture> pictures = pictureIds.stream().map(this::getById).collect(Collectors.toList());

        //删库
        this.removeBatchByIds(pictureIds);
        //删云
        cosManager.deleteBatchPicture(pictures);
    }

    /**
     * 校验登录用户权限（必须是空间创建人）
     * @param loginUser
     * @param picture
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        //公共图库,仅本人或管理员可操作
        if (spaceId == null) {
            if (!loginUser.getId().equals(picture.getUserId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }

        } else {
            //私有空间，仅空间创建人可以操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

    }

    /**
     * 删除图片
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean deletePicture(DeleteRequest deleteRequest,User loginUser) {
        //如果选择分表还需要传递spaceId，0代表公共空间 不然删除不了旗舰版空间的图片，因为原表中没有
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //先判断删除请求对象符不符合要求
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空或者参数不正确");
        }

        //判断要删除的图片存不存在
        Long id = deleteRequest.getId();
        Picture picture = this.getById(id);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //权限校验
        //checkPictureAuth(loginUser,picture);

        //事务减少空间图片数量和大小
        Boolean result = transactionTemplate.execute(status -> {
            boolean res = this.removeById(id);
            ThrowUtils.throwIf(!res, new BusinessException(ErrorCode.OPERATION_ERROR));

            //恢复用户空间图片数量和大小
            recoverSpaceCountAndSize(picture);

            return res;
        });
        //异步清理云上的图片
        this.clearPictureFile(picture);

        return result;
    }

    /**
     * 恢复用户空间图片数量和大小
     * @param picture
     */
    private void recoverSpaceCountAndSize(Picture picture) {
        Long spaceId = picture.getSpaceId();
        //需要减少空间图片大小和数量
        if (spaceId != null) {
            boolean update = spaceService.lambdaUpdate().eq(Space::getId, spaceId)
                    .setSql("totalCount = totalCount - 1")
                    .setSql("totalSize = totalSize - " + picture.getPicSize())
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);
        }
    }

    /**
     * 编辑图片
     * @param pictureEditRequest
     * @param loginUser
     * @return
     */
    @Override
    public Boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
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
        this.validPicture(picture);
        //设置编辑时间
        picture.setEditTime(new Date());

        //判断要修改的图片是否存在
        Long pictureId = pictureEditRequest.getId();
        Picture oldPicture = this.getById(pictureId);
        if (oldPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        //this.checkPictureAuth(loginUser,picture);

        //给图片填充审核参数
        this.fillReviewParams(picture, loginUser);

        //操作数据库
        boolean res = this.updateById(picture);
        ThrowUtils.throwIf(!res, new BusinessException(ErrorCode.OPERATION_ERROR));

        return res;
    }

    /**
     * 颜色搜索
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(spaceId == null,ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(picColor == null, ErrorCode.PARAMS_ERROR);
        //校验空间权限
        Space space = spaceService.getById(spaceId);
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //查询该空间下的所有图片（必须要有主色调）
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId).isNotNull(Picture::getPicColor).list();
        //如果没有图片,直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }

        //将目标色调转为Color对象
        Color color = Color.decode(picColor);

        //计算相似度并排序
        List<PictureVO> pictureVOList = pictureList.stream().sorted(Comparator.comparingDouble(picture -> {
            //提取图片的主色调
            String hexColor = picture.getPicColor();
            //如果没有主色调放到最后面
            if (StrUtil.isBlank(hexColor)) {
                return Double.MIN_VALUE;
            }

            Color pictureColor = Color.decode(hexColor);
            //越大越相似
            return -ColorSimilarUtils.calculateSimilarity(color, pictureColor);
        })).map(PictureVO::objToVo).collect(Collectors.toList());
        return pictureVOList;
    }

    /**
     * 修改我的空间内图片的主色调
     */
    @Override
    public void updateAllAve(Long spaceId) {
        //根据Id先获取我的空间信息
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId).list();
        pictureList.stream().forEach(picture -> {
            String originalUrl = picture.getOriginalUrl();
            String key = originalUrl.replace(cosClientConfig.getHost() + "/", "");
            String imageAve = cosManager.getImageAve(key);
            picture.setPicColor(imageAve);
            this.updateById(picture);
        });

    }

    /**
     * 批量编辑图片 （修改图片标签和分类和命名）
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    /*@Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        //参数校验
        validateBatchEditRequest(pictureEditByBatchRequest);
        List<String> tags = pictureEditByBatchRequest.getTags();
        String category = pictureEditByBatchRequest.getCategory();
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        String nameRule = pictureEditByBatchRequest.getNameRule();
        //验证空间权限
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //查询指定图片,仅选择需要的字段
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList).select(Picture::getId, Picture::getSpaceId).list();

        if (CollUtil.isEmpty(pictureIdList)) {
            return;
        }

        if (StrUtil.isNotBlank(nameRule)) {
            fillPictureNameRule(pictureList,nameRule);
        }

        //更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }

            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        //批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

    }*/

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        //参数校验
        validateBatchEditRequest(pictureEditByBatchRequest,loginUser);

        List<String> tags = pictureEditByBatchRequest.getTags();
        String category = pictureEditByBatchRequest.getCategory();
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        String nameRule = pictureEditByBatchRequest.getNameRule();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();

        //查询指定图片,仅选择需要的字段
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList).select(Picture::getId, Picture::getSpaceId).list();

        if (CollUtil.isEmpty(pictureIdList)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "指定的图片不存在或不属于该空间");
        }

        if (StrUtil.isNotBlank(nameRule)) {
            fillPictureNameRule(pictureList,nameRule);
        }

        //分批处理避免长事务
        int batchSize = 100;
        //异步任务集合
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        //分批
        for (int i = 0; i < pictureList.size(); i += batchSize) {
            List<Picture> batch = pictureList.subList(i, Math.min(i + batchSize, pictureList.size()));
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                batch.forEach(picture -> {
                    if (CollUtil.isNotEmpty(tags)) {
                        picture.setTags(JSONUtil.toJsonStr(tags));
                    }

                    if (StrUtil.isNotBlank(category)) {
                        picture.setCategory(category);
                    }
                });

                boolean result = this.updateBatchById(batch);

                if (!result) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量更新图片失败");
                }
            }, threadPoolExecutor);
            futures.add(future);
        }
        //主线程阻塞等待所有异步任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    }

    /**
     * 填充命名规则 格式：图片{序号}
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureNameRule(List<Picture> pictureList, String nameRule) {
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"名称解析错误");
        }

    }

    /**
     * 校验参数
     * @param pictureEditByBatchRequest
     */
    private void validateBatchEditRequest(PictureEditByBatchRequest pictureEditByBatchRequest,User loginUser) {
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();

        //参数校验
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);

        //验证空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 创建扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Parameters parameters = createPictureOutPaintingTaskRequest.getParameters();

        //校验参数
        ThrowUtils.throwIf(pictureId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(parameters == null, ErrorCode.PARAMS_ERROR);

        Picture picture = this.getById(pictureId);
        Long spaceId = picture.getSpaceId();

        if (spaceId != null) {
            //需要校验用户的权限
            validateUserAuthToSpace(loginUser, spaceId);
        }

        //构建请求参数
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        createOutPaintingTaskRequest.setInput(input);

        //调用API
        CreateOutPaintingTaskResponse response = aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
        return response;
    }

    public void validateUserAuthToSpace(User loginUser, Long spaceId) {
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作");
        }
    }
}




