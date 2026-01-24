package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

@TableName("directories")
data class DirectoryEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("kb_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var kbId: Long? = null,

    @TableField("parent_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var parentId: Long? = null,

    @TableField("name")
    var name: String? = null,

    @TableField("post_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var postId: Long? = null,

    @TableField("sort_num")
    var sortNum: Short? = null,
)
