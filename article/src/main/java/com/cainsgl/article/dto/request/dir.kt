package com.cainsgl.article.dto.request

import jakarta.validation.constraints.Min

data class CreateDirectoryRequest(
    @field:Min(value = 0,message = "知识库id不能小于0")
    val kbId: Long,
    val name: String= "新建目录",
    val parentId: Long? = null
)
data class MoveRequest(
    @field:Min(value = 0, message ="目录id不能小于0")
    val id: Long,
    @field:Min(value = 0, message ="知识库id不能小于0")
    val kbId: Long,
    val lastId: Long?,
    val parentId: Long?,
)
data class ReSortRequest(
    @field:Min(value = 0, message ="目录id不能小于0")
    val id: Long,
    @field:Min(value = 0, message ="知识库id不能小于0")
    val kbId: Long,
    val lastId: Long?,
)
data class UpdateDirectoryRequest(
    @field:Min(value = 0, message ="目录id不能小于0")
    val id: Long,
    @field:Min(value = 0, message ="知识库id不能小于0")
    val kbId: Long,
    val parentId: Long,
    val name: String?,
    //  val sortNum: Short?
)
data class DeleteDirectoryRequest(
    @field:Min(value = 0, message ="知识库id不能小于0")
    val kbId: Long,
    @field:Min(value = 0,message = "目录id不能小于0")
    val dirId: Long
)