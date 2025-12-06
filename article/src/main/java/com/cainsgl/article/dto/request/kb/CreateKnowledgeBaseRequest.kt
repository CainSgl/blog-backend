package com.cainsgl.article.dto.request.kb

import com.cainsgl.common.entity.article.ArticleStatus

data class CreateKnowledgeBaseRequest(
    val name: String,
    val status: ArticleStatus
)
