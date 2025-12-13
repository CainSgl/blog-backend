package com.cainsgl.user.dto.response

import com.cainsgl.common.entity.user.UserEntity
import java.time.OffsetDateTime

data class UserResponse(
    var id: Long,
    var username: String,
    //   var email: String,
    var nickname: String,
    var avatarUrl: String,
    var bio: String,
    var level: Int,
    //   var experience: Int,
    //   var roles: List<String>,
    var status: String,
    //   var emailVerified: Boolean,
    //   var phone: String,
    var createdAt: OffsetDateTime,
    //   var updatedAt: OffsetDateTime,
)
{
    constructor(userEntity: UserEntity) : this(
        userEntity.id!!,
        userEntity.username,
        //   userEntity.email!!,
        userEntity.nickname,
        userEntity.avatarUrl,
        userEntity.bio,
        userEntity.level,
        // userEntity.experience,
        //  userEntity.roles,
        userEntity.status,
        //  userEntity.emailVerified,
        //  userEntity.phone!!,
        userEntity.createdAt!!,
        //   userEntity.updatedAt!!,
    )
}