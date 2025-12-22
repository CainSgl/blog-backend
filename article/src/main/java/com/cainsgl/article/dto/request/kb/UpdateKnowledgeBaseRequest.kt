package com.cainsgl.article.dto.request.kb

import com.cainsgl.common.entity.article.ArticleStatus

data class UpdateKnowledgeBaseRequest(
    val id: Long,
    val name: String,
    val status: ArticleStatus
)
