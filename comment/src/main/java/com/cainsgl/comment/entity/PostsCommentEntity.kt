package com.cainsgl.comment.entity

import com.baomidou.mybatisplus.annotation.*
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

@TableName("post_comment")
data class PostsCommentEntity(
    @TableId(type = IdType.AUTO)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("content")
    var content: String? = null,

    @TableField("post_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var postId: Long? = null,

    @TableField("version")
    var version: Int? = null,

    @TableField("user_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null,

    @TableField("like_count")
    var likeCount: Int? = null,
    @TableField("reply_count")
    var replyCount: Int? = null
)