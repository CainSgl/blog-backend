package com.cainsgl.article.dto.request

import com.cainsgl.common.entity.article.ArticleStatus

data class UpdateKnowledgeBaseRequest(
    val id: Long,
    val name: String,
    val status: ArticleStatus
)
