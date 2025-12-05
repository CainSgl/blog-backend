package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.ArticleStatusTypeHandler
import com.cainsgl.common.handler.StringListTypeHandler
import java.time.OffsetDateTime

@TableName(value = "posts", autoResultMap = true)
data class PostEntity(
    @TableId(type = IdType.ASSIGN_ID)
    var id: Long? = null,

    @TableField("title")
    var title: String = "",

    @TableField("content")
    var content: String = "",

    @TableField("summary")
    var summary: String? = null,

    @TableField(value = "status", typeHandler = ArticleStatusTypeHandler::class)
    var status: ArticleStatus = ArticleStatus.DRAFT,

    @TableField("is_top")
    var isTop: Boolean = false,

    @TableField("is_recommend")
    var isRecommend: Boolean = false,

    @TableField("view_count")
    var viewCount: Long = 0,

    @TableField("like_count")
    var likeCount: Long = 0,

    @TableField("comment_count")
    var commentCount: Long = 0,

    @TableField(value = "tags", typeHandler = StringListTypeHandler::class)
    var tags: List<String> = ArrayList(),

    @TableField("user_id")
    var userId: Long? = null,

    @TableField("category_id")
    var categoryId: Long? = null,

    @TableField("seo_keywords")
    var seoKeywords: String? = null,

    @TableField("seo_description")
    var seoDescription: String? = null,

    @TableField("created_at")
    var createdAt: OffsetDateTime? =null,

    @TableField("updated_at")
    var updatedAt: OffsetDateTime? = null,

    @TableField("published_at")
    var publishedAt: OffsetDateTime? = null,

)