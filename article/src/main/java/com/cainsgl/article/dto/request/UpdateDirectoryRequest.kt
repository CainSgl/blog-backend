package com.cainsgl.article.dto.request

data class UpdateDirectoryRequest(
    val id: Long?,
    val kbId: Long?,
    val parentId: Long?,
    val name: String?,
    val sortNum: Short?
)
