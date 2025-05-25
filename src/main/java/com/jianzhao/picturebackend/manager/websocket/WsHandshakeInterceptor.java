package com.jianzhao.picturebackend.manager.websocket;

import cn.hutool.core.util.StrUtil;
import com.jianzhao.picturebackend.manager.auth.model.SpaceUserAuthManager;
import com.jianzhao.picturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.jianzhao.picturebackend.model.entity.Picture;
import com.jianzhao.picturebackend.model.entity.Space;
import com.jianzhao.picturebackend.model.entity.User;
import com.jianzhao.picturebackend.model.enums.SpaceTypeEnum;
import com.jianzhao.picturebackend.service.PictureService;
import com.jianzhao.picturebackend.service.SpaceService;
import com.jianzhao.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * ClassName: WsHandshakeInterceptor
 * Package: com.yupi.yupicturebackend.manager.websocket
 * Description:
 *  ws的请求拦截器，主要用于对权限进行验证，添加Session会话属性
 * @Author 阿小健
 * @Create 2025/5/20 15:53
 * @Version 1.0
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {
    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 拦截器,握手之前做校验
     * @param request the current request
     * @param response the current response
     * @param wsHandler the target WebSocket handler
     * @param attributes the attributes from the HTTP handshake to associate with the WebSocket
     * session; the provided attributes are copied, the original map is not used.
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        //首先判断是不是ServletServerHttpRequest的实例
        if (request instanceof ServletServerHttpRequest) {
            //获取请求参数 对参数进行校验
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            String pictureIdStr = servletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureIdStr)) {
                log.error("缺少图片参数拒绝握手");
                return false;
            }

            Long pictureId = Long.valueOf(pictureIdStr);
            //判断图片是否存在
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在,拒绝握手");
                return false;
            }

            Long spaceId = picture.getSpaceId();
            Space space = spaceService.getById(spaceId);
            if (space == null) {
                log.error("空间不存在,拒绝握手");
                return false;
            }

            if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                log.error("不是团队空间,拒绝握手");
                return false;
            }

            //校验用户是否有该图片的权限
            User loginUser = userService.getLoginUser(servletRequest);
            if (loginUser == null) {
                log.error("用户未登录,拒绝握手");
                return false;
            }

            List<String> permissionsByRole = spaceUserAuthManager.getPermissionsByRole(space, loginUser);
            if (!permissionsByRole.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("该用户没有编辑权限,拒绝握手");
                return false;
            }
            //设置attributes
            attributes.put("user",loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", pictureId);

        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
