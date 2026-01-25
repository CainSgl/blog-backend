package com.cainsgl.article.entity

import com.baomidou.mybatisplus.annotation.*
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

@TableName("post_view_history")
data class PostViewHistoryEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("user_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("post_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var postId: Long? = null,

    @TableField(value = "browse_time", fill = FieldFill.INSERT)
    var browseTime: LocalDateTime? = null
)
