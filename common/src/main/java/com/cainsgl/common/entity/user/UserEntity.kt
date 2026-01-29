package com.cainsgl.common.entity.user

import com.alibaba.fastjson2.annotation.JSONField
import com.baomidou.mybatisplus.annotation.*
import com.cainsgl.common.handler.StringListTypeHandler
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.pow

@TableName("\"user\"")
data class UserEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("username")
    var username: String? = null,

    @TableField("email")
    var email: String? = null,

    @field:JSONField(serialize = false)
    @TableField("password_hash")
    var passwordHash: String? = null,

    @TableField("nickname")
    var nickname: String? = null,

    @TableField("avatar_url")
    var avatarUrl: String? = null,

    @TableField("bio")
    var bio: String? = null,

    @TableField("level")
    var level: Int? = null,

    @TableField("gender")
    var gender: String? = null,

    @TableField("experience")
    var experience: Int? = null,

    @TableField(value = "roles", typeHandler = StringListTypeHandler::class)
    var roles: List<String>?=null,

    @TableField(value = "permissions", typeHandler = StringListTypeHandler::class)
    var permissions: List<String>?=null,

    @field:JSONField(serialize = false)
    @TableField("status")
    var status: String? = null,
    @TableField("email_verified")
    var emailVerified: Boolean? = null,
    @TableField("phone")
    var phone: String? = null,
    @TableField("used_memory")
    var usedMemory: Int? = null,
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null,
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    var updatedAt: LocalDateTime? = null,
    @TableField(value = "extra", select = false)
    var extra: String? = null
)
{
    companion object{
        val BASIC_COL= listOf("id","nickname","avatar_url","level","gender")
        val DEFAULT_ROLE=listOf("user")
        val DEFAULT_PERMISSIONS=emptyList<String>()
    }

    //到下一级的总经验值
    @TableField(exist = false)
    var nextLevelTotalExp: Int? = null
    //从现在到下一级需要的经验值
    @TableField(exist = false)
    var expToNextLevel: Int? = null


    //计算对应的成员变量
    fun calculateLevelInfo(): UserEntity
    {
        if(this.level != null&&this.experience!=null)
        {
            this.nextLevelTotalExp = (2.0.pow(this.level!! + 1) + this.level!!).toInt()
            this.expToNextLevel = max(0, this.nextLevelTotalExp!! - this.experience!!)
        }
        return this
    }

    //去除系统级别的字段，比如密码
    fun sanitizeSystemSensitiveData(): UserEntity
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
