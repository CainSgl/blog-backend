package com.cainsgl.api.user

interface UserService {
    fun mallocMemory(userId: Long, memory: Int): Boolean
}
