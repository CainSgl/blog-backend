package com.cainsgl.ai.service

import com.cainsgl.ai.dto.response.ChatMessageDTO
import com.cainsgl.ai.dto.response.ChatSessionDTO
import com.cainsgl.ai.dto.response.SendMessageResponse
import com.cainsgl.ai.entity.ChatMessageEntity
import com.cainsgl.ai.entity.ChatSessionEntity
import com.cainsgl.ai.repository.ChatMessageMapper
import com.cainsgl.ai.repository.ChatSessionMapper
import com.cainsgl.api.user.follow.UserFollowService
import com.cainsgl.common.exception.BusinessException
import com.cainsgl.common.util.user.UserHotInfoUtils.Companion.changeMessageCount
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ChatServiceImpl {
    @Resource
    lateinit var chatSessionMapper: ChatSessionMapper
    @Resource
    lateinit var chatMessageMapper: ChatMessageMapper
    @Resource
    lateinit var userFollowService: UserFollowService
    @Resource
    lateinit var redisTemplate: RedisTemplate<Any, Any>
    
    @Transactional
     fun getOrCreateSession(userId1: Long, userId2: Long): ChatSessionEntity {
        val (smallerId, largerId) = if (userId1 < userId2) userId1 to userId2 else userId2 to userId1
        
        var session = chatSessionMapper.findSessionByUsers(smallerId, largerId)
        if (session == null) {
            val senderFollowsReceiver = userFollowService.hasFollow(userId1, userId2)
            
            if (!senderFollowsReceiver) {
                throw BusinessException("需要关注对方才能发起会话")
            }
            
            session = ChatSessionEntity(
                userId1 = smallerId,
                userId2 = largerId,
                createdAt = LocalDateTime.now()
            )
            chatSessionMapper.insert(session)
        }
        return session
    }

     fun getUserSessions(userId: Long, lastId: Long? = null, pageSize: Int = 20): List<ChatSessionDTO> {
        val sessions = chatSessionMapper.findUserSessions(userId, lastId, pageSize)
        return sessions.map { session ->
            val otherUserId = if (session.userId1 == userId) session.userId2 else session.userId1
            val unreadCount = if (session.userId1 == userId) session.msg1 else session.msg2
            val deletedByOther = if (session.userId1 == userId) session.deletedByUser2 else session.deletedByUser1
            
            ChatSessionDTO(
                id = session.id!!,
                otherUserId = otherUserId,
                lastMessage = session.lastMessage,
                lastMessageTime = session.lastMessageTime,
                unreadCount = unreadCount,
                deletedByOther = deletedByOther
            )
        }
    }

    @Transactional
     fun getUnreadSessions(userId: Long, pageSize: Int = 300): List<ChatSessionDTO> {
        val sessions = chatSessionMapper.findUnreadSessions(userId, pageSize)
        
        // 清零所有未读消息计数
        if (sessions.isNotEmpty()) {
            val sessionIds = sessions.mapNotNull { it.id }
            chatSessionMapper.clearAllUnreadCount(userId, sessionIds)
        }
        
        return sessions.map { session ->
            val otherUserId = if (session.userId1 == userId) session.userId2 else session.userId1
            val unreadCount = if (session.userId1 == userId) session.msg1 else session.msg2
            val deletedByOther = if (session.userId1 == userId) session.deletedByUser2 else session.deletedByUser1
            
            ChatSessionDTO(
                id = session.id!!,
                otherUserId = otherUserId,
                lastMessage = session.lastMessage,
                lastMessageTime = session.lastMessageTime,
                unreadCount = unreadCount,
                deletedByOther = deletedByOther
            )
        }
    }

    @Transactional
     fun sendMessage(senderId: Long, receiverId: Long, content: String, isReceiverOnline: Boolean): SendMessageResponse {
        val session = getOrCreateSession(senderId, receiverId)
        
        val message = ChatMessageEntity(
            sessionId = session.id!!,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            createdAt = LocalDateTime.now()
        )
        chatMessageMapper.insert(message)
        
        // 只有在双方都未删除会话时才更新最后消息
        if (!session.deletedByUser1 && !session.deletedByUser2) {
            session.lastMessage = content
            session.lastMessageTime = message.createdAt
            chatSessionMapper.updateById(session)
        }
        
        // 如果对方不在线，增加未读消息计数
        var unreadCount = 0
        if (!isReceiverOnline) {
            // 更新 Redis 中的全局消息计数
            redisTemplate.changeMessageCount(1, receiverId)
            // 更新 session 表中的 msg_1 或 msg_2
            chatSessionMapper.incrementUnreadCount(session.id!!, receiverId)
            // 获取更新后的未读计数
            val updatedSession = chatSessionMapper.selectById(session.id!!)
            unreadCount = if (updatedSession.userId1 == receiverId) updatedSession.msg1 else updatedSession.msg2
        }
        
        return SendMessageResponse(
            messageId = message.id!!,
            isOnline = isReceiverOnline,
            unreadCount = unreadCount
        )
    }

     fun getMessages(sessionId: Long, userId: Long, last: LocalDateTime?): com.cainsgl.ai.dto.response.ChatMessagesResponse {
        val pageSize = 20
        val messages = chatMessageMapper.findMessagesBySession(sessionId, last, pageSize)
        
        // 首次查询时清除未读计数
        if (last == null) {
            chatSessionMapper.clearUnreadCount(sessionId, userId)
        }
        
        val messageDTOs = messages.map { msg ->
            ChatMessageDTO(
                id = msg.id!!,
                sessionId = msg.sessionId,
                senderId = msg.senderId,
                receiverId = msg.receiverId,
                content = msg.content,
                createdAt = msg.createdAt!!
            )
        }
        
        // 返回最后一条消息的时间戳，供下次查询使用
        val lastTimestamp = messages.lastOrNull()?.createdAt
        
        return com.cainsgl.ai.dto.response.ChatMessagesResponse(
            messages = messageDTOs,
            last = lastTimestamp
        )
    }
    

    

}
