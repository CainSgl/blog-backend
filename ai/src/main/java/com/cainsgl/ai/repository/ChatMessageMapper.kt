package com.cainsgl.ai.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.ai.entity.ChatMessageEntity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDateTime

@Mapper
interface ChatMessageMapper : BaseMapper<ChatMessageEntity> {
    fun findMessagesBySession(
        @Param("sessionId") sessionId: Long,
        @Param("last") last: LocalDateTime?,
        @Param("limit") limit: Int
    ): List<ChatMessageEntity>
}
