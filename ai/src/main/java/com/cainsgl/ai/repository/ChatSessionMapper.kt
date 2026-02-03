package com.cainsgl.ai.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.ai.entity.ChatSessionEntity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface ChatSessionMapper : BaseMapper<ChatSessionEntity> {
    fun findSessionByUsers(@Param("userId1") userId1: Long, @Param("userId2") userId2: Long): ChatSessionEntity?
    fun findUserSessions(
        @Param("userId") userId: Long,
        @Param("lastId") lastId: Long?,
        @Param("pageSize") pageSize: Int
    ): List<ChatSessionEntity>
    fun findUnreadSessions(@Param("userId") userId: Long, @Param("pageSize") pageSize: Int): List<ChatSessionEntity>
    fun clearAllUnreadCount(@Param("userId") userId: Long, @Param("sessionIds") sessionIds: List<Long>): Int
    fun incrementUnreadCount(@Param("sessionId") sessionId: Long, @Param("userId") userId: Long): Int
    fun clearUnreadCount(@Param("sessionId") sessionId: Long, @Param("userId") userId: Long): Int
    fun deleteSessionByUser(@Param("sessionId") sessionId: Long, @Param("userId") userId: Long): Int
    fun restoreSessionByUser(@Param("sessionId") sessionId: Long, @Param("userId") userId: Long): Int
    fun findDeletedSessions(
        @Param("userId") userId: Long,
        @Param("page") page: Int,
        @Param("pageSize") pageSize: Int
    ): List<ChatSessionEntity>
    fun countDeletedSessions(@Param("userId") userId: Long): Long
}
