package com.cainsgl.article.dto.request.dir

data class MoveRequest(
    val id: Long?,
    val kbId: Long?,
    val lastId: Long?,
    val parentId: Long?,
)
