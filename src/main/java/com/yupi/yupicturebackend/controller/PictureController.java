package com.yupi.yupicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.api.aliyunai.AliYunAiApi;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.yupicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.yupi.yupicturebackend.api.imagesearch.model.ImageSearchResult;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.picture.*;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebackend.model.vo.PictureTagCategory;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
@Slf4j
//@Api(tags = "图片接口")
public class PictureController {
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SpaceService spaceService;

    @Resource
    private AliYunAiApi aliYunAiApi;

    /**
     * 本地缓存 Caffeine
     */
    private final Cache<String,String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10000L)
            .expireAfterWrite(5L,TimeUnit.MINUTES)
            .build();

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
     * url上传图片
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    //@ApiOperation(value = "url上传图片")
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
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
        User loginUser = userService.getLoginUser(request);
        Boolean result = pictureService.deletePicture(deleteRequest,loginUser);
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
    public BaseResponse<PictureVO> getPictureVOById(Long id,HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            User loginUser = userService.getLoginUser(request);
            //校验权限 只有本人能看
            pictureService.checkPictureAuth(loginUser, picture);
        }

        //TODO：优化,如果图片的审核状态是未审核，则不能访问 可能需要加上管理员！
        if (PictureReviewStatusEnum.REVIEWING.getValue().equals(picture.getReviewStatus())) {
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
        Long spaceId = pictureQueryRequest.getSpaceId();

        //限制爬虫
        ThrowUtils.throwIf(pageSize > 20, new BusinessException(ErrorCode.PARAMS_ERROR));

        if (spaceId == null) {
            //普通用户只能查看已经过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            //私有空间 只有创建者可以访问
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限访问");
            }
        }

        //分页查询pictureVO
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(page, request);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 分页获取图片列表（封装类）缓存
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();
        Long spaceId = pictureQueryRequest.getSpaceId();
        //限制爬虫
        ThrowUtils.throwIf(pageSize > 20, new BusinessException(ErrorCode.PARAMS_ERROR));

        //查询的是公开图片还是私有空间图片
        if (spaceId == null) {
            //普通用户只能查看已经过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            //私有空间 只有创建者可以访问
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限访问");
            }
        }

        //构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());

        //分布式缓存key 和 本地缓存的key
        String cacheKey = "yun_picture:listPictureVOByPage:" + hashKey;
        //从本地缓存中获取数据
        String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(cacheValue)) {
            Page<PictureVO> cachePage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(cachePage);
        }

        //从Redis缓存中查询
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        //如果缓存命中,返回结果
        cacheValue = valueOps.get(cacheKey);
        if (StrUtil.isNotBlank(cacheValue)) {
            LOCAL_CACHE.put(cacheKey, cacheValue);
            Page<PictureVO> cachePage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(cachePage);
        }

        //查询数据库
        //分页查询pictureVO
        Page<Picture> page = pictureService.page(new Page<>(current, pageSize), pictureService.getQueryWrapper(pictureQueryRequest));
        //获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(page, request);
        //构建要缓存的值
        String cacheResult = JSONUtil.toJsonStr(pictureVOPage);
        //传入本地缓存
        LOCAL_CACHE.put(cacheKey,cacheResult);
        //传入缓存并且设置不同的过期时间,防止雪崩
        int cacheTime = 5 + RandomUtil.randomInt(6);
        valueOps.set(cacheKey,cacheResult,cacheTime, TimeUnit.MINUTES);
        //返回结果
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

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        Boolean res = pictureService.editPicture(pictureEditRequest,loginUser);
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

    /**
     * 批量抓取和创建图片
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        //校参
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Integer resultCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(resultCount);
    }

    /**
     * 批量删除图片
     * @param pictureIds
     * @return
     */
    @PostMapping("/deleteBatchPicture")
    public BaseResponse<Boolean> deleteBatchPicture(@RequestParam List<Long> pictureIds) {
        pictureService.deleteBatchPicture(pictureIds);
        return ResultUtils.success(true);
    }

    /**
     * 以图搜图
     * @param request
     * @return
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest request) {
        //先判空
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = request.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        //查询原图是否存在
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //调用接口方法
        String originalUrl = oldPicture.getOriginalUrl();
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImage(originalUrl);
        //返回
        return ResultUtils.success(imageSearchResults);
    }

    /**
     * 颜色搜索
     * @param searchPictureByColorRequest
     * @param request
     * @return
     */
    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 修改我的空间内图片的所有主色调（仅我自己使用）
     * @return
     */
    @PostMapping("/changeAllAve")
    public BaseResponse<Boolean> updateAllAve(Long spaceId) {
        pictureService.updateAllAve(spaceId);
        return ResultUtils.success(true);
    }

    /**
     * 批量编辑图片信息
     * @param pictureEditByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest,HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建扩图任务
     * @param request
     * @return
     */
    @PostMapping("/out_painting/create_task")
    public BaseResponse<CreateOutPaintingTaskResponse> createOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequest request,HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(request, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     * @param taskId
     * @return
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getOutPaintingTaskResponse(String taskId) {
        ThrowUtils.throwIf(taskId == null, ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse response = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(response);
    }
}