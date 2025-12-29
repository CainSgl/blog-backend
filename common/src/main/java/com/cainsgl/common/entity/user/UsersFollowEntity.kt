package com.cainsgl.common.entity.user

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

@TableName("users_follow")
data class UsersFollowEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @TableField("follower_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var followerId: Long?=null,

    @TableField("followee_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var followeeId: Long?=null,
)