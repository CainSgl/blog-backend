package com.cainsgl.article.dto.request

import com.cainsgl.common.entity.article.ArticleStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDateTime

data class CreateKnowledgeBaseRequest(
    @field:NotEmpty(message = "名称不能为空")
    val name: String,
    val status: ArticleStatus,
    val index:String?=null,
    val coverUrl:String?=null,
)
data class UpdateKnowledgeBaseRequest(
    @field:Min(value = 1, message = "知识库id非法")
    val id: Long,
    val name: String?,
    val status: ArticleStatus?,
    val content:String?,
    val coverUrl:String?
)
data class CursorKbRequest(
    val lastCreatedAt: LocalDateTime?,
    val lastLike: Int?,
    val lastId: Long?,
    @field:Max(value = 100, message = "每页数量不能超过100")
    val pageSize: Int
)