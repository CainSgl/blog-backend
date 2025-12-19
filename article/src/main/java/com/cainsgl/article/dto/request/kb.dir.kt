package com.cainsgl.article.dto.request

import com.cainsgl.common.entity.article.ArticleStatus
import jakarta.validation.constraints.Min

data class CreateKnowledgeBaseRequest(
    val name: String,
    val status: ArticleStatus
)
data class UpdateKnowledgeBaseRequest(
    @field:Min(value = 1, message = "知识库id非法")
    val id: Long,
    val name: String,
    val status: ArticleStatus
)
