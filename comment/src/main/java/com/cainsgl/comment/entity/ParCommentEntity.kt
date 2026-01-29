package com.cainsgl.comment.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

@TableName(value = "paragraph_comment")
data class ParCommentEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("user_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("data_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var dataId: Int? = null,

    @TableField("post_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var postId: Long? = null,

    @TableField("content")
    var content: String? = null,

    @TableField("version")
    var version: Int? = null,
    @TableField("like_count")
    var likeCount: Int? = null,
    @TableField("created_at")
    var createdAt: LocalDateTime? = null,
    @TableField("reply_count")
    var replyCount: Int? = null
)