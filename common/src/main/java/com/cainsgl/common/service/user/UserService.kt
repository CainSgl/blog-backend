package com.cainsgl.common.service.user

import com.cainsgl.common.entity.user.UserEntity

interface UserService {
    fun getUser(id: Long): UserEntity?
    fun updateById(userEntity: UserEntity): Boolean
}
