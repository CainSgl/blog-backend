package com.cainsgl.article.dto.request.post

data class UpdatePostRequest(
    val id: Long?=null,
    val title: String? =null,
    val content: String? = null,
    val summary: String? = null,
    val isTop: Boolean ?=null,
)
