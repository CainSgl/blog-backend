package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.*
import com.cainsgl.common.handler.ArticleStatusTypeHandler
import com.cainsgl.common.handler.StringListTypeHandler
import com.cainsgl.common.handler.VectorTypeHandler
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.OffsetDateTime

@TableName(value = "posts", autoResultMap = true)
data class PostEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @TableField("title")
    var title: String? = null,

    @TableField("content")
    var content: String ?= null,

    @TableField("summary")
    var summary: String? = null,

    @TableField(value = "status", typeHandler = ArticleStatusTypeHandler::class)
    var status: ArticleStatus? = null,

    @TableField("is_top")
    var top: Boolean ?= null,

    @TableField("is_recommend")
    var recommend: Boolean ?= null,

    @TableField("view_count")
    var viewCount: Int? = null,

    @TableField("like_count")
    var likeCount: Int? = null,

    @TableField("comment_count")
    var commentCount: Int? = null,

    @TableField(value = "tags", typeHandler = StringListTypeHandler::class)
    var tags: List<String>? = null,

    @TableField("user_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("category_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var categoryId: Long? = null,

    @TableField("seo_keywords")
    var seoKeywords: String? = null,

    @TableField("seo_description")
    var seoDescription: String? = null,
    @TableField("img")
    var img: String? = null,
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    var createdAt: OffsetDateTime?=null,
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    var updatedAt: OffsetDateTime?=null,
    @TableField("published_at")
    var publishedAt: OffsetDateTime? = null,
    @TableField("kb_id")
    @JsonSerialize(using = ToStringSerializer::class)
    var kbId: Long? = null,
    @TableField("vector", select = false,typeHandler = VectorTypeHandler::class)
    var vecotr: FloatArray? = null,
)