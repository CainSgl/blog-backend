package com.cainsgl.api.user.extra

interface UserExtraInfoService {
    fun getInterestVector(userId: Long): FloatArray?
    fun setInterestVector(userId: Long, values: FloatArray):Boolean
}
