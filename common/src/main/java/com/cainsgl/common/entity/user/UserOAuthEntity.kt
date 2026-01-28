package com.cainsgl.common.entity.user

import com.baomidou.mybatisplus.annotation.*
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

@TableName("user_oauth")
data class UserOAuthEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("user_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("provider")
    var provider: Short? = null,

    @TableField("provider_user_id")
    var providerUserId: String? = null,

    @TableField("access_token")
    var accessToken: String? = null,

    @TableField("refresh_token")
    var refreshToken: String? = null,

    @TableField("expires_at")
    var expiresAt: String? = null,

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null,

)
enum class OAuthType(val type: String, val value:Short) {
    BILIBILI("bilibili",0),
}