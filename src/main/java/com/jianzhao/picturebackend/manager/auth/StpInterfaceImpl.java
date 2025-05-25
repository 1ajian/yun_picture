package com.jianzhao.picturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.jianzhao.picturebackend.exception.ThrowUtils;
import com.jianzhao.picturebackend.model.entity.Space;
import com.jianzhao.picturebackend.model.entity.User;
import com.jianzhao.picturebackend.model.enums.SpaceTypeEnum;
import com.jianzhao.picturebackend.exception.BusinessException;
import com.jianzhao.picturebackend.exception.ErrorCode;
import com.jianzhao.picturebackend.manager.auth.model.SpaceUserAuthManager;
import com.jianzhao.picturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.jianzhao.picturebackend.model.entity.Picture;
import com.jianzhao.picturebackend.model.entity.SpaceUser;
import com.jianzhao.picturebackend.model.enums.SpaceRoleEnum;
import com.jianzhao.picturebackend.service.PictureService;
import com.jianzhao.picturebackend.service.SpaceService;
import com.jianzhao.picturebackend.service.SpaceUserService;
import com.jianzhao.picturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jianzhao.picturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * ClassName: StpInterfaceImpl
 * Package: com.yupi.yupicturebackend.manager.auth
 * Description:
 *  自定义权限加载接口实现类
 *
 *  保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
 * @Author 阿小健
 * @Create 2025/5/17 17:05
 * @Version 1.0
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 从请求中获取上下文对象
     * @return
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        //获取请求对象
        HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());

        SpaceUserAuthContext authRequest = null;

        //判断是Post还是get 并获取请求的参数 因为每个请求参数对象 都有Id,只是通过JSON转换后,我们并不知道是哪一个对象是Picture 还是Space
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }

        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            //例如  /api/picture/xxx
            String requestUri = request.getRequestURI();
            //  picture/xxx
            String partUri = requestUri.replace(contextPath + "/", "");
            //  picture
            String moduleName = StrUtil.subBefore(partUri, "/", false);

            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;

                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }


    /**
     * 返回一个账号所拥有的权限码集合
     * @param loginId  账号id
     * @param loginType 账号类型
     * @return
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        //如果登录类型不是space，直接返回空权限列表
        if (!loginType.equals(StpKit.SPACE_TYPE)){
            return Collections.emptyList();
        }

        //获取所有的管理员权限
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());

        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();

        //如果所有的字段都为空,表示查询公共图库
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }

        //根据用户id去 Space Session中获取当前登录的用户
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //获取当前登录的用户Id
        Long userId = loginUser.getId();

        //从上下文对象中获取  空间用户
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            String spaceRole = spaceUser.getSpaceRole();
            List<String> permissionsByRole = spaceUserAuthManager.getPermissionsByRole(spaceRole);
            return permissionsByRole;
        }

        //获取空间用户Id
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);

            //判断当前用户是否是属于该空间
            boolean exists = spaceUserService.lambdaQuery().eq(SpaceUser::getUserId, userId)
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId()).exists();
            if (!exists) {
                return Collections.emptyList();
            }

            //查询对应角色的权限
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

        //根据空间Id和图片Id进行判断
        Long spaceId = authContext.getSpaceId();
        //空间Id不存在
        if (spaceId == null) {
            Long pictureId = authContext.getPictureId();
            //图片id也不存在
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }

            Picture picture = pictureService.lambdaQuery().eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId).one();

            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }

            spaceId = picture.getSpaceId();
            //公共图库
            if (spaceId == null) {
                //只有创建者或者管理员有 管理员的权限
                if (picture.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    //只有读权限
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        //获取space对象
        Space space= spaceService.getById(spaceId);
        if (space == null) {
            space = authContext.getSpace();
        }
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        //如果是私有空间
        if (space.getSpaceType().equals(SpaceTypeEnum.PRIVATE.getValue())) {
            if (space.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            } else {
                return Collections.emptyList();
            }
        } else {
            //团队
            Long id = space.getId();
            spaceUser = spaceUserService.lambdaQuery().eq(SpaceUser::getSpaceId, id).eq(SpaceUser::getUserId, userId).one();
            if (spaceUser == null) {
                return Collections.emptyList();
            }
            String spaceRole = spaceUser.getSpaceRole();
            List<String> permissionsByRole = spaceUserAuthManager.getPermissionsByRole(spaceRole);
            return permissionsByRole;

        }
    }

    //判断上下文对象的属性值是否全为空
    private boolean isAllFieldsNull(SpaceUserAuthContext authContext) {
        if (authContext == null) {
            return true;
        }

        //获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(authContext.getClass()))//获取所有的运行时类字段
                .map(field -> ReflectUtil.getFieldValue(authContext,field))//获取所有的属性对象
                .allMatch(ObjUtil::isEmpty);//进行全匹配
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return null;
    }
}
