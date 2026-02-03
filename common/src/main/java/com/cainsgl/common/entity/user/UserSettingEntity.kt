package com.cainsgl.common.entity.user

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.JsonTypeHandler

@TableName("user_setting", autoResultMap = true)
data class UserSettingEntity(
    @TableId(type = IdType.INPUT)
    var userId: Long? = null,

    @TableField(typeHandler = JsonTypeHandler::class)
    var json: Map<String, Any>? = null
)
