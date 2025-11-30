package com.cainsgl.common.util

import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.common.entity.user.UserEntity

object UserUtils
{
    private const val USER_INFO_KEY = "userInfo"

    fun getUserInfo(): UserEntity?
    {
        val o = StpUtil.getSession().get(USER_INFO_KEY)
        if (o != null)
        {
            return o as UserEntity
        }
        return null
    }

    fun setUserInfo(userEntity: UserEntity)
    {
        StpUtil.getSession().set(USER_INFO_KEY, userEntity)
    }
}
