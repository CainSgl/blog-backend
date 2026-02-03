package com.cainsgl.ai.service

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.ai.entity.ChatSessionEntity
import com.cainsgl.ai.repository.ChatSessionMapper
import org.springframework.stereotype.Service

@Service
class ChatSessionImpl : ServiceImpl<ChatSessionMapper, ChatSessionEntity>()
{}