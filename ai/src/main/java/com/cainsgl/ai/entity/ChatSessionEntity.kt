package com.cainsgl.ai.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

@TableName("chat_session")
data class ChatSessionEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @TableField("user_id_1")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId1: Long,
    @TableField("user_id_2")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId2: Long,
    @TableField("last_message")
    var lastMessage: String? = null,
    @TableField("last_message_time")
    var lastMessageTime: LocalDateTime? = null,
    @TableField("created_at")
    var createdAt: LocalDateTime? = null,
    @TableField("deleted_by_user1")
    var deletedByUser1: Boolean = false,
    @TableField("deleted_by_user2")
    var deletedByUser2: Boolean = false,
    @TableField("msg_1")
    var msg1: Int = 0,
    @TableField("msg_2")
    var msg2: Int = 0
)
