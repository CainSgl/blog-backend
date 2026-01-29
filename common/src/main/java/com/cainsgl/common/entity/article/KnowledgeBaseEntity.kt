package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.ArticleStatusTypeHandler
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

@TableName("knowledge_base")
data class KnowledgeBaseEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("user_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("name")
    var name: String? = null,

    @TableField("created_at")
    var createdAt: LocalDateTime? = null,

    @TableField(value = "status", typeHandler = ArticleStatusTypeHandler::class)
    var status: ArticleStatus? = null,
    @TableField(value = "index", select = false)
    var index: String? = null,
    @TableField(value = "like_count")
    var likeCount: Int? = null,
    @TableField(value = "cover_url")
    var coverUrl: String? = null,
    @TableField(value = "post_count")
    var postCount: Int? = null,
)
