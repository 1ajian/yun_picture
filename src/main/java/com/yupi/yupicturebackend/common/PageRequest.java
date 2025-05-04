package com.yupi.yupicturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * ClassName: PageRequest
 * Package: com.yupi.yupicturebackend.common
 * Description:
 *      分页请求包装类
 * @Author 阿小健
 * @Create 2025/4/22 16:08
 * @Version 1.0
 */
@Data
public class PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 当前页
     */
    private long current = 1;
    /**
     * 页大小
     */
    private long pageSize = 10;
    /**
     * 排序字段
     */
    private String sortField;
    /**
     * 排序规则(默认降序)
     */
    private String sortOrder = "descend";
}