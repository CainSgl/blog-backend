package com.cainsgl.common.config

import cn.dev33.satoken.stp.StpInterface
import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.common.entity.user.UserEntity
import org.springframework.stereotype.Component

/**
 * SaToken 权限接口实现
 * 从 Session 中获取用户的角色和权限
 */
@Component
class StpInterfaceImpl : StpInterface
{
    companion object
    {
        private const val USER_INFO_KEY = "userInfo"
    }
    override fun getPermissionList(loginId: Any, loginType: String): List<String>
    {
        val user = StpUtil.getSession().get(USER_INFO_KEY) as UserEntity? ?: return ArrayList()
        return user.permissions
    }

    override fun getRoleList(loginId: Any, loginType: String): List<String>
    {
        val user = StpUtil.getSession().get(USER_INFO_KEY) as UserEntity? ?: return ArrayList()
        return user.roles
    }
}
