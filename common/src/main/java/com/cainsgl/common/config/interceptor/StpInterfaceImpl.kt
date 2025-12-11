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
    override fun afterPropertiesSet() {
        logger.info{"StpInterfaceImpl init"}
        val getPermissionGroup = "SELECT role, permission_list FROM permission_group"
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery(getPermissionGroup).use { rs ->
                    val tempMap = mutableMapOf<String, List<String>>()
                    while (rs.next()) {
                        val role = rs.getString("role")
                        val permissionArray = rs.getArray("permission_list")
                        val permissions = if (permissionArray != null) {
                            val array = permissionArray.array as? Array<*>
                            array?.mapNotNull { it as? String } ?: emptyList()
                        } else {
                            emptyList()
                        }
                        tempMap[role] = permissions
                    }
                    rolePermissionMap = tempMap
                }
            }
        }
        logger.info{"Permission groups loaded: $rolePermissionMap"}
    }
    companion object
    {
        private const val USER_INFO_KEY = "userInfo"
    }
    override fun getPermissionList(loginId: Any, loginType: String): List<String>
    {
        val user = StpUtil.getSession().get(USER_INFO_KEY) as UserEntity? ?: return ArrayList()
        // 创建可变列表用于合并权限
        val allPermissions = user.permissions.toMutableList()
        // 根据role获取permissions并合并
        for (role in user.roles)
        {
            val permissions = rolePermissionMap[role] ?: continue
            allPermissions.addAll(permissions)
        }
        return allPermissions.distinct() // 去重后返回
    }

    override fun getRoleList(loginId: Any, loginType: String): List<String>
    {
        val user = StpUtil.getSession().get(USER_INFO_KEY) as UserEntity? ?: return ArrayList()
        return user.roles
    }
}