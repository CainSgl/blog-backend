package com.cainsgl.user.dto.request

data class CheckInRequest(
    val userId: Long? = null  // 可选，默认使用当前登录用户
)
