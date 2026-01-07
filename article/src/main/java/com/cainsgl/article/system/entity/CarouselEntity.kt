package com.cainsgl.article.system.entity

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDate

@TableName(value = "carousels")
data class CarouselEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @TableField("date")
    var date: LocalDate? = null,
    @TableField("title")
    var title: String? = null,
    @TableField("description")
    var description: String? = null,
    @TableField("url")
    var url: String? = null,
    @TableField("cover_url")
    var coverUrl: String? = null,
    @TableField("color")
    var color: String? = null,
)