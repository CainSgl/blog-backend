package com.cainsgl.user.dto.response

import com.cainsgl.common.entity.user.UserEntity

data class LoginResponse(
    val token: String,
    val userInfo: UserEntity?,
    val isNew:Boolean=false
)
data class ErrorLoginResponse(
    val msg: String,
    val needCaptcha: Boolean = false,
    val captchaImage: String? = null, // Base64 编码的验证码图片
    var modal:String?=null,
    var token:String?=null
)
data class RegisterSuccessResponse(
    val token: String,
)