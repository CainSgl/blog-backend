package com.cainsgl.article.dto.request

import jakarta.validation.constraints.Min

data class CreateDirectoryRequest(
    @field:Min(value = 1,message = "知识库id非法")
    val kbId: Long,
    val name: String= "新建目录",
    val parentId: Long? = null
)
data class MoveRequest(
    @field:Min(value = 1, message ="目录id非法")
    val id: Long,
    @field:Min(value = 1, message ="知识库id非法")
    val kbId: Long,
    val lastId: Long?,
    val parentId: Long?,
)
data class ReSortRequest(
    @field:Min(value = 1, message ="目录id非法")
    val id: Long,
    @field:Min(value = 1, message ="知识库id非法")
    val kbId: Long,
    val lastId: Long?,
)
data class UpdateDirectoryRequest(
    @field:Min(value = 1, message ="目录id非法")
    val id: Long,
    @field:Min(value = 1, message ="知识库id非法")
    val kbId: Long,
    @field:Min(value = 1, message ="父级目录id非法")
    val parentId: Long?,
    val name: String?,
    //  val sortNum: Short?
)
data class DeleteDirectoryRequest(
    @field:Min(value = 1, message ="知识库id非法")
    val kbId: Long,
    @field:Min(value = 1,message = "目录id非法")
    val dirId: Long
)