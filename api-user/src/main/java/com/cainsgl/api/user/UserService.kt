package com.cainsgl.api.user

interface UserService {
    fun mallocMemory(userId: Long, memory: Int): Boolean
    fun createNotice(targetId: Long, type: Int, userId: Long, targetUser: Long): Boolean
}
