package com.cainsgl.api.article.kb

interface KnowledgeBaseService {
    fun addKbLikeCount(kbId: Long, addCount: Int)
}
