package com.jianzhao.picturebackend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.jianzhao.picturebackend.model.entity.Space;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ClassName: SpaceVO
 * Package: com.yupi.yupicturebackend.model.vo
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/5/9 15:11
 * @Version 1.0
 */
@Data
public class SpaceVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
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
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();


    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     * @param spaceVO
     * @return
     */
    public static Space voToObj(SpaceVO spaceVO) {
        if (spaceVO == null) {
            return null;
        }

        Space space = new Space();
        BeanUtil.copyProperties(spaceVO, space);
        return space;
    }

    /**
     * 对象转封装类
     * @param space
     * @return
     */
    public static SpaceVO objToVo(Space space) {
        if (space == null) {
            return null;
        }

        SpaceVO spaceVO = new SpaceVO();
        BeanUtil.copyProperties(space,spaceVO);
        return spaceVO;
    }

}
