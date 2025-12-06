package com.cainsgl.article.dto.request.dir

data class CreateDirectoryRequest(
    val kbId: Long?,
    val name: String= "新建目录",
    val parentId: Long? = null
)
