package com.cainsgl.common.config.interceptor

import cn.dev33.satoken.stp.StpInterface
import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.common.entity.user.UserEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

@Component
@ConditionalOnClass(org.postgresql.Driver::class)
class StpInterfaceImpl : StpInterface, InitializingBean
{

    @Resource
    lateinit var dataSource: DataSource
    private lateinit var rolePermissionMap: Map<String, List<String>>
    override fun afterPropertiesSet()
    {
        logger.info { "StpInterfaceImpl init" }
        val getPermissionGroup = "SELECT role, permission_list FROM permission_group"
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery(getPermissionGroup).use { rs ->
                    val tempMap = mutableMapOf<String, List<String>>()
                    while (rs.next())
                    {
                        val role = rs.getString("role")
                        val permissionArray = rs.getArray("permission_list")
                        val permissions = if (permissionArray != null)
                        {
                            val array = permissionArray.array as? Array<*>
                            array?.mapNotNull { it as? String } ?: emptyList()
                        } else
                        {
                            emptyList()
                        }
                        tempMap[role] = permissions
                    }
                    rolePermissionMap = tempMap
                }
            }
        }
        logger.info { "Permission groups loaded: $rolePermissionMap" }
    }

    companion object
    {
        const val USER_ROLE_KEY = "user:role"
        const val USER_PERMISSIONS_KEY = "user:permission"
    }

    override fun getPermissionList(loginId: Any, loginType: String): List<String>
    {
        val roles = StpUtil.getSession().get(USER_ROLE_KEY).toStringList()
        val extraPermissionList= StpUtil.getSession().get(USER_ROLE_KEY).toStringList().toMutableList()
        // 根据role获取permissions并合并
        for (role in roles)
        {
            val permissions = rolePermissionMap[role] ?: continue
            extraPermissionList.addAll(permissions)
        }
        return extraPermissionList.distinct() // 去重后返回
    }

    override fun getRoleList(loginId: Any, loginType: String): List<String>
    {
        val roles = StpUtil.getSession().get(USER_ROLE_KEY).toStringList()
        return roles
    }
}

private fun Any?.toStringList(): List<String>
{
    return when (this)
    {
        is List<*> -> this.map { element ->
            // 规避NPE
            element?.toString() ?: ""
        }
        else       -> emptyList()
    }
}