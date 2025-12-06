package com.cainsgl.article.dto.request.post

data class CreatePostRequest(
    val title: String="新建文章",
    val kbId: Long?,
    val parentId: Long?,
)
