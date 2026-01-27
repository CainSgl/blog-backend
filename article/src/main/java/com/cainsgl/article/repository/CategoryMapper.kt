package com.cainsgl.article.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.article.CategoryEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface CategoryMapper : BaseMapper<CategoryEntity>
