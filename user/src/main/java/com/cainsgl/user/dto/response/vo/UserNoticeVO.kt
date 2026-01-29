package com.cainsgl.user.dto.response.vo

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

data class UserNoticeVO(
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @field:JsonSerialize(using = ToStringSerializer::class)
    var targetId: Long? = null,
    
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,
    
    @field:JsonSerialize(using = ToStringSerializer::class)
    var targetUser: Long? = null,
    
    var checked: Boolean? = null,

    var createdAt: LocalDateTime? = null,
    // target_user 的基本信息
    var targetUserNickname: String? = null,
    var targetUserAvatarUrl: String? = null,
    var targetUserLevel: Int? = null,
    var targetUserGender: String? = null
)
