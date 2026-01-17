package com.cainsgl.common.entity.user

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

@TableName("user_collect")
class UserCollectEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @TableField("user_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,
    @TableField("target_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var targetId: Long? = null,
    @TableField("group_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var groupId: Long? = null,
)
{

}