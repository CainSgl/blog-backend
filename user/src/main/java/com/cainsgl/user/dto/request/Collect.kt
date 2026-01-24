package com.cainsgl.user.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotEmpty

data class PostGroupRequest(
    val type: String,
    val name: String = "未命名分组",
    val description: String = "",
    val publish: Boolean = false,
)

data class PutGroupRequest(
    val id: Long,
    val name: String?=null,
    val description: String?=null,
    val publish: Boolean?=null,
)

data class PageCollectRequest(
    val page: Long = 1,
    @field:Max(100, message = "每页数量不能超过100")
    val pageSize: Long = 20,
    val id: Long
)
data class PostCollectRequest(
    val targetId: Long,
    val groupId: Long,

)