package com.cainsgl.user.dto.response

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

data class FollowUserResponse(
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    var nickname: String? = null,
    var avatarUrl: String? = null,
    var level: Int? = null,
    var gender: String? = null
)