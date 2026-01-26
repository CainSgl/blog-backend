package com.cainsgl.api.user.extra

import com.cainsgl.common.entity.user.UserExtraInfoEntity

interface UserExtraInfoService {
    fun getInterestVector(userId: Long): FloatArray?
    fun setInterestVector(userId: Long, values: FloatArray):Boolean
    fun saveCount(userExtraInfo: UserExtraInfoEntity): Boolean
}
