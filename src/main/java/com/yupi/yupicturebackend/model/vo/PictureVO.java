package com.yupi.yupicturebackend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.model.entity.Picture;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ClassName: PictureVO
 * Package: com.yupi.yupicturebackend.model.vo
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/25 15:11
 * @Version 1.0
 */
@Data
public class PictureVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 原图
     */
    private String originalUrl;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;


    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 分类
     */
    private String category;

    /**
     * 文件体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();



    private static final long serialVersionUID = 1L;

    /**
     * 普通对象转vo
     * @param picture
     * @return
     */
    public static PictureVO objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }

        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture,pictureVO);
        String pictureTagJson = picture.getTags();
        pictureVO.setTags(JSONUtil.toList(pictureTagJson, String.class));

        return pictureVO;
    }

    /**
     * vo转普通对象
     * @param pictureVO
     * @return
     */
    public static Picture voToObj(PictureVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }

        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureVO, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));

        return picture;
    }
}
