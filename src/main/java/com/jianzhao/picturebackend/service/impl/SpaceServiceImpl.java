package com.jianzhao.picturebackend.service.impl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import com.jianzhao.picturebackend.exception.ThrowUtils;
import com.jianzhao.picturebackend.mapper.SpaceMapper;
import com.jianzhao.picturebackend.model.dto.space.SpaceAddRequest;
import com.jianzhao.picturebackend.model.entity.Space;
import com.jianzhao.picturebackend.model.entity.SpaceUser;
import com.jianzhao.picturebackend.model.entity.User;
import com.jianzhao.picturebackend.model.enums.SpaceLevelEnum;
import com.jianzhao.picturebackend.model.enums.SpaceRoleEnum;
import com.jianzhao.picturebackend.model.enums.SpaceTypeEnum;
import com.jianzhao.picturebackend.model.vo.SpaceVO;
import com.jianzhao.picturebackend.model.vo.UserVO;
import com.jianzhao.picturebackend.service.SpaceService;
import com.jianzhao.picturebackend.service.SpaceUserService;
import com.jianzhao.picturebackend.service.UserService;
import com.jianzhao.picturebackend.model.dto.space.SpaceQueryRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
* @author 86135
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-05-09 15:01:14
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserService spaceUserService;

    //@Resource
    //@Lazy
    //private DynamicShardingManager dynamicShardingManager;

    Map<Long,Object> lockMap = new ConcurrentHashMap<>();
    /**
     * 校验空间数据
     * @param space
     * @param add
     */
    @Override
    public void validSpace(Space space, boolean add) {
        //判空
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);

        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

        //是创建的情况
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称不能为空");
            }

            if (ObjUtil.isNull(spaceLevelEnum)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }

            if (ObjUtil.isNull(spaceTypeEnum)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "空间类型不能为空");
            }
        }

        //是修改数据时,如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }

        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }

    /**
     * 根据空间级别，自动填充限额
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        //根据空间级别，自动填充限额
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        long maxCount = spaceLevelEnum.getMaxCount();
        long maxSize = spaceLevelEnum.getMaxSize();
        if (space.getMaxCount() == null) {
            space.setMaxCount(maxCount);
        }

        if (space.getMaxSize() == null) {
            space.setMaxSize(maxSize);
        }
    }

    /**
     * 创建空间服务
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        //TODO:可以优化,管理员允许创建多个空间

        //先进行实体类和DTO的转换
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest,space);
        //设置默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }

        if (spaceAddRequest.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }

        if (spaceAddRequest.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        //填充数据
        fillSpaceBySpaceLevel(space);
        //数据校验
        validSpace(space, true);
        //权限校验
        if (spaceAddRequest.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        /*//针对每个用户进行加锁
        String userId = String.valueOf(loginUser.getId()).intern();
        synchronized (userId) {
            //TODO:编程式事务：保证事务的提交在加锁的范围内
            Long spaceId = transactionTemplate.execute(status -> {
                //验证每个用户只有一个私有空间
                boolean exists = this.lambdaQuery().eq(Space::getUserId, loginUser.getId()).exists();
                if (exists) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "每个用户只能创建一个私有的空间");
                }
                //返回新写入的id
                boolean save = this.save(space);
                ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
                return space.getId();
            });
            return Optional.ofNullable(spaceId).orElse(-1L);
        }*/

        //TODO:使用ConcurrentHashMap来保证获取锁之后会释放
        Long userId = loginUser.getId();
        //设置用户Id
        space.setUserId(userId);
        Object lock = lockMap.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            try {
                //对于普通用户来说 只能有一个私有空间和一个团队空间
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, loginUser.getId())
                        .eq(Space::getSpaceType,space.getSpaceType())
                        .exists();
                if (exists) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "每个用户只能创建一个私有的空间");
                }
                //返回新写入的id
                boolean save = this.save(space);
                ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);

                //对团队空间而言，还需要加上团队成员记录
                if (spaceAddRequest.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    boolean result = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR,"团队成员记录失败");
                }
                //创建图片分表(picture_spaceId)
                //dynamicShardingManager.createSpacePictureTable(space);

                return space.getId();
            } finally {
                lockMap.remove(userId);
            }
        }
    }

    /**
     * 将Space转SpaceVO对象
     * @param space
     * @param request
     * @return
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = new SpaceVO();
        BeanUtil.copyProperties(space, spaceVO);

        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }

        return spaceVO;

    }

    /**
     * 获取查询QueryWrapper对象
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {

         Long id = spaceQueryRequest.getId();
         Long userId = spaceQueryRequest.getUserId();
         String spaceName = spaceQueryRequest.getSpaceName();
         Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
         String sortField = spaceQueryRequest.getSortField();
         String sortOrder = spaceQueryRequest.getSortOrder();
         Integer spaceType = spaceQueryRequest.getSpaceType();

        QueryWrapper<Space> spaceQueryWrapper = new QueryWrapper<>();
         spaceQueryWrapper.eq(ObjUtil.isNotEmpty(id),"id", id)
                 .eq(ObjUtil.isNotEmpty(userId),"userId", userId)
                 .like(StrUtil.isNotBlank(spaceName),"spaceName", spaceName)
                 .eq(ObjUtil.isNotEmpty(spaceLevel),"spaceLevel", spaceLevel)
                 .eq(ObjUtil.isNotNull(spaceType), "spaceType",spaceType)
                 .orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);

        return spaceQueryWrapper;
    }

    /**
     * 将分页 Space转 SpaceVO
     * @param spacePage
     * @param request
     * @return
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        //构建返回参数
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());

        //获取原始记录
        List<Space> spaceList = spacePage.getRecords();

        //获取用户VO信息转为map方便查询
        Map<Long, UserVO> map = spaceList.stream().map(space -> {
            Long userId = space.getUserId();
            User user = userService.getById(userId);
            return userService.getUserVO(user);
        }).distinct().collect(Collectors.toMap(UserVO::getId, userVO -> userVO));


        //构建结果
        List<SpaceVO> spaceVOList = new ArrayList<>();
        for (Space space : spaceList) {
            Long userId = space.getUserId();
            SpaceVO spaceVO = new SpaceVO();
            BeanUtil.copyProperties(space, spaceVO);
            spaceVO.setUser(map.get(userId));
            spaceVOList.add(spaceVO);
        }


        //设置结果集
        spaceVOPage.setRecords(spaceVOList);
        //返回
        return spaceVOPage;
    }

    /**
     * 对空间进行权限验证
     * @param loginUser
     * @param space
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"只有管理员和空间创建者可以访问");
        }
    }
}




