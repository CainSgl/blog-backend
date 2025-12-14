package com.cainsgl.article.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.article.PostEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface PostMapper : BaseMapper<PostEntity> {
    
    /**
     * 批量查询文章基础信息（排除content等大字段）
     */
    fun selectBasicInfoByIds(ids: List<Long>): List<PostEntity>

    fun selectVectorById(id:Long):FloatArray?
}
