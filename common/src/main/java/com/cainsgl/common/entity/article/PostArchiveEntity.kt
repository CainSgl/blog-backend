package com.cainsgl.common.entity.article

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.OffsetDateTime

@TableName("post_archives")
data class PostArchiveEntity(

    @TableId(type = IdType.ASSIGN_ID)
    var id: Long? = null,

    @TableField("post_title")
    var postTitle: String = "",

    @TableField(value = "post_data")
    var postData: String = "{}",

    @TableField("archive_type")
    var archiveType: String = "",

    @TableField("archive_time")
    var archiveTime: OffsetDateTime? =null,

    @TableField("operator_id")
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