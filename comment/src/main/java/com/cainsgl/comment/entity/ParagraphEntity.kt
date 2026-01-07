package com.cainsgl.comment.entity

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

@TableName(value = "paragraphs")
data class ParagraphEntity(
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