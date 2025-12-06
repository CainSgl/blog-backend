package com.cainsgl.article.dto.request.post

data class UpdatePostRequest(
    val id: Long?,
    val title: String? = "",
    val content: String? = "",
    val summary: String? = null,
    val status: String?=null,
    val isTop: Boolean ?=null,
)
