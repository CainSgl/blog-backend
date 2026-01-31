package com.cainsgl.user.dto.response

data class CheckInResponse(
    val success: Boolean,              // true=签到成功, false=今天已签到
    val expGained: Int? = null,        // 本次获得的经验（已签到时为null）
    val checkInDays: List<Int>? = null // 本月已签到的日期列表（已签到时为null）
)
