package com.cainsgl.common.entity.user

import com.alibaba.fastjson2.annotation.JSONField
import com.baomidou.mybatisplus.annotation.*
import com.cainsgl.common.handler.StringListTypeHandler
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.math.max
import kotlin.math.pow

@TableName("users")
open class UserEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("username")
    var  username: String = "",

    @TableField("email")
    var email: String? = null,

    @JSONField(serialize = false)
    @TableField("password_hash")
    var passwordHash: String? = null,

    @TableField("nickname")
    var nickname: String = "",

    @TableField("avatar_url")
    var avatarUrl: String = "",

    @TableField("bio")
    var bio: String = "",

    @TableField("level")
    var level: Int = 0,

    @TableField("experience")
    var experience: Int = 0,

    @TableField(value = "roles", typeHandler = StringListTypeHandler::class)
    var roles: List<String> = ArrayList(),

    @TableField(value = "permissions", typeHandler = StringListTypeHandler::class)
    var permissions: List<String> = ArrayList(),

    @JSONField(serialize = false)
    @TableField("status")
    var status: String = "",

    @TableField("email_verified")
    var emailVerified: Boolean = false,

    @TableField("phone")
    var phone: String? = null,
    @TableField("used_memory")
    var usedMemory: String? = null,
    /**
     * 这两个字段如果是在数据库是一定存在的
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    var createdAt: OffsetDateTime?=null,
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    var updatedAt: OffsetDateTime?=null
)
{

    //到下一级的总经验值
    @TableField(exist = false)
    var nextLevelTotalExp: Int?=null
    //从现在到下一级需要的经验值
    @TableField(exist = false)
    var expToNextLevel: Int?=null


    //计算对应的成员变量
    fun calculateLevelInfo():UserEntity
    {
        this.nextLevelTotalExp = (2.0.pow(this.level + 1) + this.level).toInt()
        this.expToNextLevel = max(0, this.nextLevelTotalExp!! - this.experience)
        return this
    }
    //去除系统级别的字段，比如密码
    fun sanitizeSystemSensitiveData():UserEntity
    {
        this.passwordHash = null
        return this
    }
    fun isActive(): Boolean
    {
        return "active" == this.status
    }

    data class Extra(var bannedTime: LocalDateTime? = null)
}
