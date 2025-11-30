package com.cainsgl.user.dto.response

import com.cainsgl.common.entity.user.UserEntity

data class LoginResponse(
    /**
     * 登录token
     */
    var token: String? = null,

    /**
     * 用户信息
     */
    var userInfo: UserEntity? = null
)
