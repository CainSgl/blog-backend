package com.cainsgl.user.dto.response

import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.common.entity.user.UserExtraInfoEntity
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.springframework.beans.BeanUtils
import java.time.LocalDateTime

//这个是给别人看的，屏蔽一些用户的字段
data class UserGetResponse(
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long?,
    var username: String?,
    var nickname: String?,
    var avatarUrl: String?,
    var bio: String?,
    var level: Int?,
    var status: String?,
    var createdAt: LocalDateTime?,
    var gender: String,
    val followerCount: Int?,
    var followingCount: Int?,
    var likeCount: Int?,
    var postCount:Int?,
    var articleViewCount: Int? = null,
    var usedMemory: Int?,
)
{
    constructor(userEntity: UserEntity, hotInfo: UserExtraInfoEntity? = null) : this(
        userEntity.id,
        userEntity.username,
        userEntity.nickname,
        userEntity.avatarUrl,
        userEntity.bio,
        userEntity.level,
        userEntity.status,
        userEntity.createdAt,
        userEntity.gender ?: "",
        hotInfo?.followerCount,
        hotInfo?.followingCount,
        hotInfo?.likeCount,
        hotInfo?.postCount,
        hotInfo?.articleViewCount,
        userEntity.usedMemory
    )
}

data class UserCurrentResponse(
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    var username: String ?=null,
    var email: String? = null,
    var nickname: String? =null,
    var avatarUrl: String? = null,
    var bio: String? = null,
    var level: Int? =null,
    var gender: String? = null,
    var experience: Int? = null,
    var roles: List<String>? = null,
    var permissions: List<String>? = null,
    var status: String? = null,
    var emailVerified: Boolean? = null,
    var phone: String? = null,
    var usedMemory: String? = null,
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null,
    var extra: String? = null,
    var followerCount: Int? = null,
    var followingCount: Int? = null,
    var likeCount: Int? = null,
    var commentCount: Int? = null,
    var postCount: Int? = null,
    var articleViewCount: Int? = null,
    var nextLevelTotalExp:Int?=null,
    var expToNextLevel: Int? = null,
    val msgCount: Int? = null,
    var msgReplyCount: Int? = null,
    var msgLikeCount: Int? = null,
    var msgReportCount: Int? = null,
    var msgMessageCount: Int? = null,
)
{
    constructor(user: UserEntity, hotInfo: UserExtraInfoEntity) : this()
    {
        BeanUtils.copyProperties(user, this)
        BeanUtils.copyProperties(hotInfo, this)
    }
}


