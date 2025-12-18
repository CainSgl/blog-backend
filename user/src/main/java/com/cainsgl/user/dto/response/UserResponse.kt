package com.cainsgl.user.dto.response

import com.cainsgl.common.entity.user.UserEntity
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.OffsetDateTime
//这个是给别人看的，屏蔽一些用户的字段
data class UserResponse(
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long,
    var username: String,
    var nickname: String,
    var avatarUrl: String,
    var bio: String,
    var level: Int,
    var status: String,
    var createdAt: OffsetDateTime,
)
{
    constructor(userEntity: UserEntity) : this(
        userEntity.id!!,
        userEntity.username,
        userEntity.nickname,
        userEntity.avatarUrl,
        userEntity.bio,
        userEntity.level,
        userEntity.status,
        userEntity.createdAt!!,
    )
}