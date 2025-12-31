package com.cainsgl.common.dto.request

import jakarta.validation.constraints.Min

data class OnlyId(
    @field:Min(value = 0,message = "id非法")
    val id: Long
)
data class CursorList(
    @field:Min(value=1, message = "id非法")
    val id:Long,
    @field:Min(value=0, message = "id非法")
    val lastId:Long=0
)