package com.cainsgl.user.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty



data class UserLogPostRequest(
    @field: NotEmpty(message = "用户行为不能为空")
    val action: String,
    @field:Min(value=1, message = "id非法")
    val id:Long
)

data class UpdateUserRequest(
    val nickname: String?=null,
    val avatarUrl: Long?=null,
    val bio: String?=null,
    val gender: String?=null,
    val username: String?=null,
)