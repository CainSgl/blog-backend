package com.cainsgl.common.entity.article

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.OffsetDateTime

@TableName("post_archives")
data class PostArchiveEntity(

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("post_title")
    var postTitle: String? =null,

    @TableField(value = "post_data")
    var postData: String?=null,

    @TableField("archive_type")
    var archiveType: String?=null,

    @TableField("archive_time")
    var archiveTime: OffsetDateTime? =null,

    @TableField("operator_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var operatorId: Long? = null,

    @TableField("remark")
    var remark: String? = null,
    @TableField("kb_id")
    var kbId: Long? = null
)
{
    constructor(postEntity:PostEntity) : this()
    {
        this.postData = JSON.toJSONString(postEntity)
        this.postTitle=postEntity.title
        this.operatorId=postEntity.userId
    }


}