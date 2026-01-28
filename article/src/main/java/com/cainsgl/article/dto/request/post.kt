package com.cainsgl.article.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.LocalDateTime

data class CreatePostRequest(
    val title: String = "新建文章",
    @field:Min(value = 1, message = "知识库id非法")
    val kbId: Long,
    @field:Min(1, message = "挂载的目录id非法")
    val parentId: Long?,
)

data class PubPostRequest(
    @field:Min(1,  message="公布的文章id非法")
    val id: Long,
    val status: String,
)
data class SearchEsPostRequest(
    var query: String,
    @field:Max(value = 30, message = "error")
    var size:Int=20,
    var useTag: Boolean = false,
    var useContent: Boolean = false,
    var searchAfter: List<Any>? = null
)

data class SearchPostRequest(
    var query: String,
    var vectorOffset: Double?,
    var page: Int? = 0,
    @field:Max(value = 100, message = "每页数量不能超过100")
    var size: Int? = 20
)

data class UpdatePostRequest(
    @field:Min(value = 1, message = "文章id非法")
    val id: Long,
    val title: String? = null,
    val content: String? = null,
    val summary: String? = null,
    val img: String? = null,
    val isTop: Boolean? = null,
    val tags: List<String>? = null,
)

data class CursorPostRequest(
    val lastUpdatedAt: LocalDateTime?,
    val lastLikeRatio: Double?,
    val lastId: Long?,
    @field:Max(value = 100, message = "每页数量不能超过100")
    val pageSize: Int
)
