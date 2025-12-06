package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.ArticleStatusTypeHandler
import java.time.OffsetDateTime

@TableName("knowledge_bases")
data class KnowledgeBaseEntity(
    @TableId(type = IdType.ASSIGN_ID)
    var id: Long? = null,

    @TableField("user_id")
    var userId: Long? = null,

    @TableField("name")
    var name: String = "",

    @TableField("created_at")
    var createdAt: OffsetDateTime? = null,

    @TableField(value = "status", typeHandler = ArticleStatusTypeHandler::class)
    var status: ArticleStatus? = null,
)
