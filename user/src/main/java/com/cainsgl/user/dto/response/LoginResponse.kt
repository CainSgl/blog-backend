package com.cainsgl.user.dto.response

import com.cainsgl.common.entity.user.UserEntity

data class LoginResponse(
    val token: String,
    val userInfo: UserEntity
)
