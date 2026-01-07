package com.cainsgl.article.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import org.apache.ibatis.annotations.Mapper
import java.time.LocalDateTime

@Mapper
interface KnowledgeBaseMapper : BaseMapper<KnowledgeBaseEntity> {
    fun selectFirstPage(pageSize: Int): List<KnowledgeBaseEntity>

    fun selectKbByCursor(lastCreatedAt: LocalDateTime, lastLike: Int, lastId: Long, pageSize: Int): List<KnowledgeBaseEntity>

}
