package com.cainsgl.article.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface KnowledgeBaseMapper : BaseMapper<KnowledgeBaseEntity> {

}
