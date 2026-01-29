package com.cainsgl.comment.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDate

@TableName(value = "reply")
data class ReplyEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("par_comment_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var parCommentId: Long? = null,

    @TableField("user_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("content")
    var content: String? = null,

    @TableField("like_count")
    var likeCount: Int? = null,

    @TableField("created_at")
    var createdAt: LocalDate? = null,

    @TableField("post_comment_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var postCommentId: Long? = null,

    @TableField("reply_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var replyId: Long? = null,
)