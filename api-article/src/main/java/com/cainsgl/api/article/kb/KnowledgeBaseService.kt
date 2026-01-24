package com.cainsgl.api.article.kb

import com.cainsgl.common.entity.article.KnowledgeBaseEntity

interface KnowledgeBaseService {
    fun addKbLikeCount(kbId: Long, addCount: Int)
    fun getByIds(ids: List<Long>): List<KnowledgeBaseEntity>
}
