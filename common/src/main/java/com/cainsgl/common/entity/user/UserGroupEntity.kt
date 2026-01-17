package com.cainsgl.common.entity.user

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

@TableName("user_collect_group")
class UserGroupEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @TableField("user_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,
    @TableField("name")
    var name: String? = null,
    @TableField("type")
    var type: Int? = null,
    @TableField("publish")
    var publish: Boolean? = null,
)
{

}