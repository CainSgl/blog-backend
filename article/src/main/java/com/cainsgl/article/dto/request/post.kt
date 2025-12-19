package com.cainsgl.article.dto.request

import jakarta.validation.constraints.Min

data class CreatePostRequest(
    val title: String = "新建文章",
    @field:Min(value = 1, message = "知识库id非法")
    val kbId: Long,
    @field:Min(1,  message="挂载的目录id非法")
    val parentId: Long?,
)

data class PubPostRequest(
    @field:Min(1,  message="公布的文章id非法")
    val id: Long
)


data class SearchPostRequest(
    var query: String,
    var vectorOffset: Double?
)

data class UpdatePostRequest(
    @field:Min(value = 1, message = "文章id非法")
    val id: Long,
    val title: String? =null,
    val content: String? = null,
    val summary: String? = null,
    val isTop: Boolean ?=null,
)
