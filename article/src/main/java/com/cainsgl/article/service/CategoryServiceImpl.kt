package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.article.CategoryEntity
import com.cainsgl.common.service.article.category.CategoryService
import com.cainsgl.article.repository.CategoryMapper
import org.springframework.stereotype.Service

@Service
class CategoryServiceImpl : ServiceImpl<CategoryMapper, CategoryEntity>(), CategoryService, IService<CategoryEntity> {

    /**
     * 获取分类信息
     * @param id
     * @return
     */
    fun getCategory(id: Long): CategoryEntity? {
        return baseMapper.selectById(id)
    }

}
