package com.cainsgl.common.entity.user

import com.alibaba.fastjson2.annotation.JSONField
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.JsonTypeHandler
import java.time.LocalDateTime

@TableName(value = "user_logs", autoResultMap = true)
data class UserLogEntity(
    @TableId(type = IdType.ASSIGN_ID)
    var id: Long? = null,

    @TableField("user_id")
    var userId: Long? = null,

    @TableField("action")
    var action: String? = null,

    @TableField("device")
    var device: String? = null,

    @TableField(value = "info", typeHandler = JsonTypeHandler::class)
    var info: Map<String, Any>? = null,

    @JSONField(serialize = false)
    @TableField("created_at")
    var createdAt: LocalDateTime? = null,
)
{
    fun isValidAction(): Boolean
    {
        if (action == null)
        {
            return false
        }
        return  validAction(this.action!!)
    }

    companion object
    {
        val ACTIONS_SET = mapOf(
            "article" to listOf("view", "like", "report", "dislike", "unlike"),
            "user" to listOf("follow", "unfollow"),
            "system" to listOf(),
            "kb" to listOf("like","unlike")
        )
        fun validAction(action: String): Boolean
        {
            val parts = action.split(".", limit = 2)
            if (parts.size != 2) return false
            val (type, actionName) = parts
            val allowedActions = ACTIONS_SET[type] ?: return false
            return allowedActions.contains(actionName)
        }
    }

}
