package com.cainsgl.ai.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

@TableName("chat_message")
data class ChatMessageEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @TableField("session_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var sessionId: Long,
    @TableField("sender_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var senderId: Long,
    @TableField("receiver_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var receiverId: Long,
    @TableField("content")
    var content: String,
    @TableField("created_at")
    var createdAt: LocalDateTime? = null
)
