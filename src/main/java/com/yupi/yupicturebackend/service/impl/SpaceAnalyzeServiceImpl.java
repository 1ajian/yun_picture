package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.analyze.*;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.space.analyze.*;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.SpaceAnalyzeService;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * ClassName: SpaceAnalyzeServiceImpl
 * Package: com.yupi.yupicturebackend.service.impl
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/15 12:40
 * @Version 1.0
 */
@Service
@Slf4j
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    /**
     * 获取空间使用分析数据
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        //如果是全空间或者是公开空间分析,那么需要找图片表
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            //需要对登录用户进行权限验证
            checkSpaceAnalyzeAuth(loginUser,spaceUsageAnalyzeRequest);
            //填充参数
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest,queryWrapper);
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream().mapToLong(picSize -> (long) picSize).sum();
            long usedCount = pictureObjList.size();
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            return spaceUsageAnalyzeResponse;

        } else {
            //如果是私有空间分析,那么需要找空间表
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR,"未找到对应的空间");
            //获取空间信息
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            Long totalSize = space.getTotalSize();
            Long totalCount = space.getTotalCount();
            Long maxCount = space.getMaxCount();
            Long maxSize = space.getMaxSize();
            double countUsageRatio = NumberUtil.round(totalCount * 100.0 / maxCount, 2).doubleValue();
            double sizeUsageRatio = NumberUtil.round(totalSize * 100.0 / maxSize, 2).doubleValue();
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();

            spaceUsageAnalyzeResponse.setUsedSize(totalSize);
            spaceUsageAnalyzeResponse.setUsedCount(totalCount);
            spaceUsageAnalyzeResponse.setMaxSize(maxSize);
            spaceUsageAnalyzeResponse.setMaxCount(maxCount);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            return spaceUsageAnalyzeResponse;

        }
    }

    /**
     * 空间图片分类分析
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {

        //校验参数
        checkSpaceAnalyzeAuth(loginUser,spaceCategoryAnalyzeRequest);
        //构建请求参数
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("category","COUNT(*) as count","SUM(picSize) as totalSize").groupBy("category");
        //填充参数
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        //转换结果
        List<SpaceCategoryAnalyzeResponse> resultList = pictureService.getBaseMapper().selectMaps(queryWrapper).stream().map(result -> {
            String category = result.get("category") == null ? "未分类" : result.get("category").toString();
            Long count = (Long) result.get("count");

            BigDecimal size = (BigDecimal) result.get("totalSize");
            long totalSize = size.longValue();

            return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
        }).collect(Collectors.toList());

        return resultList;
    }

    /**
     * 空间标签分析数据
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        //权限验证
        checkSpaceAnalyzeAuth(loginUser, spaceTagAnalyzeRequest);

        //select * from picture where [spaceId = null] | [spaceId = x] | [不用where]
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>().select("tags");
        //填充参数
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        //请求 结果是List[JSON]
        List<String> tagsList = pictureService.getBaseMapper().selectObjs(queryWrapper).stream().filter(ObjUtil::isNotNull).map(Object::toString).collect(Collectors.toList());
        //合并所有标签并统计使用次数 //平铺操作
        Map<String, Long> resultMap = tagsList.stream().flatMap(tag -> JSONUtil.toList(tag, String.class).stream()).collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        //构建返回结果
        return resultMap.entrySet().stream()
                .sorted((o1, o2) -> Long.compare(o2.getValue(), o1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(),entry.getValue())).collect(Collectors.toList());

    }

    /**
     * 空间大小分析
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        //验证参数
        checkSpaceAnalyzeAuth(loginUser, spaceSizeAnalyzeRequest);

        //构建请求参数 select * from picture where [space is null] | [space = x] | [没有where]
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>().select("picSize");

        //对参数进行填充
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        List<Long> resultList = pictureService.getBaseMapper().selectObjs(queryWrapper).stream().map(picSize -> (Long) picSize).collect(Collectors.toList());

        LinkedHashMap<String, Long> resultMap = new LinkedHashMap<>();
        resultMap.put("<100KB", resultList.stream().filter(result -> result < 100L * 1024).count());
        resultMap.put("100KB-500KB", resultList.stream().filter(result -> (result >= 100L * 1024 && result < 500L * 1024)).count());
        resultMap.put("500KB-1MB", resultList.stream().filter(result -> (result >= 500L * 1024 && result < 1024L * 2024)).count());
        resultMap.put(">1MB",resultList.stream().filter(result -> (result >= 1024L * 1024)).count());

        return resultMap.entrySet().stream().map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(),entry.getValue())).collect(Collectors.toList());
    }

    /**
     * 用户上传行为分析
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
         Long userId = spaceUserAnalyzeRequest.getUserId();
         String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();

        //权限校验
        checkSpaceAnalyzeAuth(loginUser, spaceUserAnalyzeRequest);

        //构建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(userId != null, "userId",spaceUserAnalyzeRequest.getUserId());

        //填充参数
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        //匹配返回字段
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m-%d') as period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        //分组和升序
        queryWrapper.groupBy("period").orderByAsc("period");

        //查询数据库并根据要求返回数据
        return pictureService.getBaseMapper().selectMaps(queryWrapper).stream().map(result -> {
            String period = (String) result.get("period");
            Long count = (Long) result.get("count");
            return new SpaceUserAnalyzeResponse(period,count);
        }).collect(Collectors.toList());
    }

    /**
     * 空间使用排行分析
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        //教参
        //只有管理员可以使用
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //构建请求参数
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName","userId","totalSize")
                .orderByDesc("totalSize")
                .last("limit " + spaceRankAnalyzeRequest.getTopN());

        List<Space> result = spaceService.list(queryWrapper);

        return result;
    }


    /**
     * 检查权限
     * @param loginUser
     * @param spaceAnalyzeRequest
     */
    private void checkSpaceAnalyzeAuth(User loginUser, SpaceAnalyzeRequest spaceAnalyzeRequest) {
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            //身份必须是管理员
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else {
            //先进行参数的校验
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            //必须是管理员或者空间的创建者
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 根据查询范围创建查询条件
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        } else if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
        } else if (spaceAnalyzeRequest.getSpaceId() != null && spaceAnalyzeRequest.getSpaceId() > 0){
            queryWrapper.eq("spaceId", spaceAnalyzeRequest.getSpaceId());
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查找的范围或者指定范围有误");
        }
    }


}
