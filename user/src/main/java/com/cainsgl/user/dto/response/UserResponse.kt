package com.cainsgl.user.dto.response

import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.common.entity.user.UserExtraInfoEntity
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.springframework.beans.BeanUtils
import java.time.OffsetDateTime

//这个是给别人看的，屏蔽一些用户的字段
data class UserGetResponse(
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

final class UserCurrentResponse :UserEntity
{
    val followerCount: Int
    var followingCount: Int
    var likeCount: Int
    var commentCount: Int
    var postCount: Int
    var articleViewCount: Int
    constructor(user:UserEntity,hotInfo:UserExtraInfoEntity)
    {
        followerCount=hotInfo.followerCount
        followingCount=hotInfo.followingCount
        likeCount=hotInfo.likeCount
        commentCount=hotInfo.commentCount
        postCount=hotInfo.postCount
        articleViewCount=hotInfo.articleViewCount
        BeanUtils.copyProperties(user, this)
    }
}


