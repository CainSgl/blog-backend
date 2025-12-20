package com.cainsgl.common.entity.file

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDate

@TableName("file_urls")
data class FileUrlEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer::class)
    var shortUrl: Long? = null,

    @TableField("user_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("url")
    var url: String? = null,
    @TableField("name")
    var name: String? = null,
    @TableField("file_size")
    var fileSize: Int? = null,
    @TableField("created_at")
    var createdAt: LocalDate? = null,
)