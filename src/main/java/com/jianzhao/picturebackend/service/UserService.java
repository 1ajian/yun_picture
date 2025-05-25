package com.jianzhao.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jianzhao.picturebackend.model.dto.user.UserQueryRequest;
import com.jianzhao.picturebackend.model.entity.User;
import com.jianzhao.picturebackend.model.vo.CaptchaVo;
import com.jianzhao.picturebackend.model.vo.LoginUserVO;
import com.jianzhao.picturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 86135
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-04-23 20:03:34
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param shareCode 其他用户邀请码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword,String shareCode);

    /**
     * 获取图片验证码
     * @return
     */
    CaptchaVo getCaptcha();

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword,String captchaKey,String captchaCode, HttpServletRequest request);


    /**
     * 获取用户脱敏的已登录信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVo(User user);

    /**
     * 获取当前登录用户信息
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取加密密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户注销
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后单个对象信息
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏用户信息列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 查询请求转为 QueryWrapper 对象
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 是否为管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 兑换会员
     * @param user
     * @param vipCode
     * @return
     */
    boolean exchangeVip(User user, String vipCode);

}
