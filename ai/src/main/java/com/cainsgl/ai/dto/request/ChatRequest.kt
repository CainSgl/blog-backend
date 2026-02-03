package com.cainsgl.ai.dto.request

import java.time.LocalDateTime

// 仅用于 REST API 查询
data class GetMessagesRequest(
    val sessionId: Long,
    val last: LocalDateTime? = null  // 上次查询最后一条消息的时间戳，首次查询传null
)
