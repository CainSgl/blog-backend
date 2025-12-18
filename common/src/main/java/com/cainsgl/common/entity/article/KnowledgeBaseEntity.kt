package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.ArticleStatusTypeHandler
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.OffsetDateTime

@TableName("knowledge_bases")
data class KnowledgeBaseEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("user_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("name")
    var name: String? = null,

    @TableField("created_at")
    var createdAt: OffsetDateTime? = null,

    @TableField(value = "status", typeHandler = ArticleStatusTypeHandler::class)
    var status: ArticleStatus? = null,
)
