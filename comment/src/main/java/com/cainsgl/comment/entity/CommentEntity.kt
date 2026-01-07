package com.cainsgl.comment.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDate

@TableName(value = "paragraph_comments")
data class CommentEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("user_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("data_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var dataId: Int? = null,

    @TableField("post_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var postId: Long? = null,

    @TableField("content")
    var content: String? = null,

    @TableField("version")
    var version: Int? = null,
    @TableField("like_count")
    var likeCount: Int? = null,
    @TableField("created_at")
    var createdAt: LocalDate? = null,
)