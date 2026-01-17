package com.cainsgl.user.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotEmpty

data class PostGroupRequest(
    val type: String,
    val name: String = "未命名分组",
)

data class PutGroupRequest(
    val id: Long,
    @field:NotEmpty(message = "分组名称不能为空")
    val name: String,
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