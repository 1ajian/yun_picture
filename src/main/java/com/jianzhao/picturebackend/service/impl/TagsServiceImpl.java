package com.jianzhao.picturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jianzhao.picturebackend.model.entity.Tags;
import com.jianzhao.picturebackend.service.TagsService;
import com.jianzhao.picturebackend.mapper.TagsMapper;
import org.springframework.stereotype.Service;

/**
* @author 86135
* @description 针对表【tags(图片标签表)】的数据库操作Service实现
* @createDate 2025-05-23 14:17:08
*/
@Service
public class TagsServiceImpl extends ServiceImpl<TagsMapper, Tags>
    implements TagsService{

}




