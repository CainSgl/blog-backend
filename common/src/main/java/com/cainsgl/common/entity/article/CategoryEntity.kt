package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.OffsetDateTime

@TableName("categories")
data class CategoryEntity(
    @TableId(type = IdType.ASSIGN_ID)
    var id: Long? = null,
    @TableField("name")
    var name: String = "",
    @TableField("parent_id")
    var parentId: Long? = null,
    @TableField("created_at")
    var createdAt: OffsetDateTime? = null
) {

}