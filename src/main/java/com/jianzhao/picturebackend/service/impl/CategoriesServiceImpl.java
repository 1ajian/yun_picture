package com.jianzhao.picturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzhao.picturebackend.model.entity.Categories;
import com.jianzhao.picturebackend.service.CategoriesService;
import com.jianzhao.picturebackend.mapper.CategoriesMapper;
import org.springframework.stereotype.Service;

/**
* @author 86135
* @description 针对表【categories(图片分类表)】的数据库操作Service实现
* @createDate 2025-05-23 14:17:08
*/
@Service
public class CategoriesServiceImpl extends ServiceImpl<CategoriesMapper, Categories>
    implements CategoriesService{

}




