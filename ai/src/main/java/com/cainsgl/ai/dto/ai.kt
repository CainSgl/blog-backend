package com.cainsgl.ai.dto

import java.time.LocalDateTime

data class TagCore(
    val tag:String?,
    val core:Float?,
)
data class AiMessage(
    val role:String,
    val content:String,
    val createTime:LocalDateTime=LocalDateTime.now(),
)