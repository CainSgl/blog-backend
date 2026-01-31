package com.cainsgl.user.dto.request

import jakarta.validation.constraints.NotEmpty

data class RegisterRequest(
    var step: Int?,
    var username: String?,
    var token: String?,
    var email: String?,
    var password: String?,
)

data class UserLoginRequest(
    @field:NotEmpty(message = "Username cannot be empty") val account: String,
    @field:NotEmpty(message = "Password cannot be empty") val password: String,
    //用于防刷
    val captcha: String? = null,
)
