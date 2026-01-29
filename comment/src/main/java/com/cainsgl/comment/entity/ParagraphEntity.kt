package com.cainsgl.comment.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

@TableName(value = "paragraph")
data class ParagraphEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @TableField("post_id")
    var postId: Long? = null,

    @TableField("count")
    var count: Int? = null,

    @TableField("data_id")
    var dataId: Int? = null,

    @TableField("version")
    var version: Int? = null,

){
    companion object {
        val BASE_COL= listOf("data_id","count")
    }
}