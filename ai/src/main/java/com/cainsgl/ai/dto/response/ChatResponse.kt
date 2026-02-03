package com.cainsgl.ai.dto.response

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

data class ChatSessionDTO(
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long,
    @field:JsonSerialize(using = ToStringSerializer::class)
    val otherUserId: Long,
    val lastMessage: String?,
    val lastMessageTime: LocalDateTime?,
    val unreadCount: Int = 0,
    val deletedByOther: Boolean = false  // 对方是否删除了会话（用于判断是否被拉黑）
)

data class ChatMessageDTO(
    @field:JsonSerialize(using = ToStringSerializer::class)
    val id: Long,
    @field:JsonSerialize(using = ToStringSerializer::class)
    val sessionId: Long,
    @field:JsonSerialize(using = ToStringSerializer::class)
    val senderId: Long,
    @field:JsonSerialize(using = ToStringSerializer::class)
    val receiverId: Long,
    val content: String,
    val createdAt: LocalDateTime
)

data class ChatMessagesResponse(
    val messages: List<ChatMessageDTO>,
    val last: LocalDateTime?  // 本次返回的最后一条消息的时间戳，用于下次查询
)

/**
 * WebSocket 消息数据传输对象
 * 
 * 支持的消息类型：
 * - "message": 发送聊天消息
 * - "typing": 输入状态通知
 * - "checkOnline": 查询在线状态（客户端发送）
 * - "onlineStatus": 在线状态响应（服务端返回）
 * 
 * 使用示例：
 * 
 * 1. 发送消息：
 *    {"type": "message", "receiverId": 123, "content": "你好", "sessionId": 456}
 * 
 * 2. 输入状态：
 *    {"type": "typing", "receiverId": 123, "atTyping": true}
 * 
 * 3. 查询单个用户在线状态：
 *    {"type": "checkOnline", "receiverId": 123}
 *    响应：{"type": "onlineStatus", "receiverId": 123, "isOnline": true}
 * 
 * 4. 批量查询在线状态：
 *    {"type": "checkOnline", "userIds": [123, 456, 789]}
 *    响应：{"type": "onlineStatus", "onlineStatus": {"123": true, "456": false, "789": true}}
 */
data class WebSocketMessage(
    /** 消息类型：message, typing, checkOnline, onlineStatus */
    val type: String,
    
    /** 会话ID（发送消息时使用） */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val sessionId: Long? = null,
    
    /** 发送者用户ID */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val senderId: Long? = null,
    
    /** 接收者用户ID */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val receiverId: Long? = null,
    
    /** 消息内容 */
    val content: String? = null,
    
    /** 消息ID（服务端返回） */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val messageId: Long? = null,
    
    /** 是否正在输入（typing 消息使用） */
    val atTyping: Boolean? = null,
    
    /** 消息时间戳 */
    val timestamp: LocalDateTime = LocalDateTime.now(),
    
    /** 在线状态（onlineStatus 响应使用，单个用户查询） */
    val isOnline: Boolean? = null,
    
    /** 批量查询的用户ID列表（checkOnline 请求使用） */
    val userIds: List<Long>? = null,
    
    /** 批量在线状态结果（onlineStatus 响应使用，批量查询） */
    val onlineStatus: Map<Long, Boolean>? = null
)

/**
 * 发送消息响应数据传输对象
 * 
 * 用于返回消息发送结果，包含消息ID、对方在线状态和未读消息数
 */
data class SendMessageResponse(
    /** 消息ID */
    @field:JsonSerialize(using = ToStringSerializer::class)
    val messageId: Long,
    
    /** 对方是否在线 */
    val isOnline: Boolean,
    
    /** 对方的未读消息数（仅在对方不在线时有值，在线时为0） */
    val unreadCount: Int = 0
)
