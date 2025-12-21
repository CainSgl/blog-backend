package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.*
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.OffsetDateTime

@TableName("posts_history")
data class PostHistoryEntity(
    @TableId(type = IdType.NONE)
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @TableField("user_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,
    @TableField("post_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var postId: Long? = null,

    @TableField("content", select = false)
    var content: String? = null,

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    var createdAt: OffsetDateTime? = null
)