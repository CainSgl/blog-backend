package com.cainsgl.ai.websocket

import com.cainsgl.ai.dto.response.WebSocketMessage
import com.cainsgl.ai.service.ChatServiceImpl
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.PongMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

@Component
class ChatWebSocketHandler : TextWebSocketHandler() {

    @Resource
    lateinit var chatService: ChatServiceImpl
    
    @Resource
    lateinit var objectMapper: ObjectMapper
    
    // 用户ID -> WebSocket会话映射
    private val userSessions = ConcurrentHashMap<Long, WebSocketSession>()



    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.attributes["userId"] as? Long
        if (userId != null) {
            userSessions[userId] = session
            logger.info { "用户 $userId 建立 WebSocket 连接" }
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val userId = session.attributes["userId"] as? Long ?: return
            
            // 处理心跳 ping
            if (message.payload == "ping") {
                session.sendMessage(TextMessage("pong"))
                return
            }
            
            val wsMessage = objectMapper.readValue(message.payload, WebSocketMessage::class.java)
            
            when (wsMessage.type) {
                "message" -> handleSendMessage(userId, wsMessage)
                "typing" -> handleTyping(userId, wsMessage)
                "checkOnline" -> handleCheckOnline(userId, wsMessage)
            }
        } catch (e: Exception) {
            logger.error(e) { "处理 WebSocket 消息失败" }
        }
    }

    override fun handlePongMessage(session: WebSocketSession, message: PongMessage) {
        // 收到客户端的 pong 响应，连接保持活跃
        logger.debug { "收到 pong 消息" }
    }

    private fun handleSendMessage(senderId: Long, wsMessage: WebSocketMessage) {
        val receiverId = wsMessage.receiverId ?: return
        val content = wsMessage.content ?: return

        // 检查接收者是否在线
        val receiverOnline = isUserOnline(receiverId)

        // 保存消息到数据库，并处理未读计数
        val result = chatService.sendMessage(senderId, receiverId, content, receiverOnline)
        
        // 构造响应消息
        val response = WebSocketMessage(
            type = "message",
            sessionId = wsMessage.sessionId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            messageId = result.messageId
        )
        
        // 发送给接收者（如果在线）
        if (receiverOnline) {
            sendToUser(receiverId, response)
        }
        
        // 发送确认给发送者
        sendToUser(senderId, response)
    }
    
    /**
     * 检查指定用户是否在线
     * 
     * 判断依据：
     * - 用户是否建立了 WebSocket 连接
     * - WebSocket 连接是否处于打开状态
     * 
     * @param userId 要检查的用户ID
     * @return Boolean true=在线，false=离线
     */
    fun isUserOnline(userId: Long): Boolean =userSessions[userId]?.isOpen ?: false

    private fun handleTyping(userId: Long, wsMessage: WebSocketMessage) {
        val receiverId = wsMessage.receiverId ?: return
        
        val response = WebSocketMessage(
            type = "typing",
            senderId = userId,
            atTyping=wsMessage.atTyping,
        )
        
        sendToUser(receiverId, response)
    }

    /**
     * 处理在线状态查询
     * 
     * 支持两种查询方式：
     * 
     * 1. 单个用户查询：
     *    发送消息：{"type": "checkOnline", "receiverId": 123456}
     *    接收响应：{"type": "onlineStatus", "receiverId": 123456, "isOnline": true}
     * 
     * 2. 批量用户查询：
     *    发送消息：{"type": "checkOnline", "userIds": [123456, 789012, 345678]}
     *    接收响应：{"type": "onlineStatus", "onlineStatus": {"123456": true, "789012": false, "345678": true}}
     * 
     * 使用场景：
     * - 实时查询对方是否在线
     * - 聊天过程中动态检查在线状态
     * - 需要低延迟的在线状态更新
     * 
     * @param userId 当前用户ID（查询发起者）
     * @param wsMessage WebSocket 消息，包含 receiverId 或 userIds
     */
    private fun handleCheckOnline(userId: Long, wsMessage: WebSocketMessage) {
        // 单个用户在线状态查询
        if (wsMessage.receiverId != null) {
            val isOnline = isUserOnline(wsMessage.receiverId)
            val response = WebSocketMessage(
                type = "onlineStatus",
                receiverId = wsMessage.receiverId,
                isOnline = isOnline
            )
            sendToUser(userId, response)
        }
        // 批量用户在线状态查询
        else if (wsMessage.userIds != null) {
            val onlineStatus = wsMessage.userIds.associateWith { targetUserId ->
                isUserOnline(targetUserId)
            }
            val response = WebSocketMessage(
                type = "onlineStatus",
                onlineStatus = onlineStatus
            )
            sendToUser(userId, response)
        }
    }

    private fun sendToUser(userId: Long, message: WebSocketMessage) {
        userSessions[userId]?.let { session ->
            if (session.isOpen) {
                try {
                    val json = objectMapper.writeValueAsString(message)
                    session.sendMessage(TextMessage(json))
                } catch (e: Exception) {
                    logger.error(e) { "发送消息给用户 $userId 失败" }
                }
            }
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val userId = session.attributes["userId"] as? Long
        if (userId != null) {
            userSessions.remove(userId)
            logger.info { "用户 $userId 断开 WebSocket 连接" }
        }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        logger.error(exception) { "WebSocket 传输错误" }
    }

}
