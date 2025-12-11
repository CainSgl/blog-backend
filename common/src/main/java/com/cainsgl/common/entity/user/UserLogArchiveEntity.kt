package com.cainsgl.common.entity.user

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.JsonTypeHandler
import java.time.LocalDateTime

@TableName(value = "user_log_archives", autoResultMap = true)
data class UserLogArchiveEntity(
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

    @TableField("created_at")
    var createdAt: LocalDateTime? = null
)
