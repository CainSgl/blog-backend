package com.cainsgl.article.dto.request

import com.cainsgl.common.entity.article.ArticleStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class CreateKnowledgeBaseRequest(
    val name: String,
    val status: ArticleStatus
)
data class UpdateKnowledgeBaseRequest(
    @field:Min(value = 1, message = "知识库id非法")
    val id: Long,
    val name: String?,
    val status: ArticleStatus?,
    val content:String?,
    val coverUrl:String?
)
data class KnowledgeBaseListRequest(
    @field:Min(value = 1, message = "页码必须大于0")
    val page: Long = 1,
    @field:Min(value = 1, message = "每页数量必须大于0")
    @field:Max(value = 30, message = "每页数量不能超过30")
    val size: Long = 10,
    val userId: Long,
    val simple:Boolean=true,
)