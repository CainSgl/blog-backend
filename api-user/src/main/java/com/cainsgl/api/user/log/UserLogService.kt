package com.cainsgl.api.user.log

interface UserLogService {
    fun loadLogsToRedis(value: Int): String
}
