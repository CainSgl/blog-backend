package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.*
import com.cainsgl.common.handler.ArticleStatusTypeHandler
import com.cainsgl.common.handler.StringListTypeHandler
import com.cainsgl.common.handler.VectorTypeHandler
import java.time.OffsetDateTime

@TableName(value = "posts", autoResultMap = true)
data class PostEntity(
    @TableId(type = IdType.ASSIGN_ID)
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
    var viewCount: Long? = null,

    @TableField("like_count")
    var likeCount: Long? = null,

    @TableField("comment_count")
    var commentCount: Long? = null,

    @TableField(value = "tags", typeHandler = StringListTypeHandler::class)
    var tags: List<String>? = null,

    @TableField("user_id")
    var userId: Long? = null,

    @TableField("category_id")
    var categoryId: Long? = null,

    @TableField("seo_keywords")
    var seoKeywords: String? = null,

    @TableField("seo_description")
    var seoDescription: String? = null,

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    var createdAt: OffsetDateTime?=null,
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    var updatedAt: OffsetDateTime?=null,
    @TableField("published_at")
    var publishedAt: OffsetDateTime? = null,
    @TableField("kb_id")
    var kbId: Long? = null,
    @TableField("vector", select = false,typeHandler = VectorTypeHandler::class)
    var vecotr: FloatArray? = null,
)
