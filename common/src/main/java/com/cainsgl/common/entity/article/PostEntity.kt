package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.*
import com.cainsgl.common.handler.ArticleStatusTypeHandler
import com.cainsgl.common.handler.StringListTypeHandler
import com.cainsgl.common.handler.VectorTypeHandler
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

@TableName(value = "posts", autoResultMap = true)
data class PostEntity(
    @TableId(type = IdType.ASSIGN_ID)
    @field:JsonSerialize(using = ToStringSerializer::class)
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
    @TableField("star_count")
    var starCount: Int? = null,
    @TableField("comment_count")
    var commentCount: Int? = null,

    @TableField(value = "tags", typeHandler = StringListTypeHandler::class)
    var tags: List<String>? = null,

    @TableField("user_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("category_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var categoryId: Long? = null,

    @TableField("seo_keywords")
    var seoKeywords: String? = null,

    @TableField("seo_description")
    var seoDescription: String? = null,
    @TableField("img")
    var img: String? = null,
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    var createdAt: LocalDateTime?=null,
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    var updatedAt: LocalDateTime?=null,
    @TableField("published_at")
    var publishedAt: LocalDateTime? = null,
    @TableField("kb_id")
    @field:JsonSerialize(using = ToStringSerializer::class)
    var kbId: Long? = null,
    @TableField("version")
    var version: Int? = null,
    @TableField(value="like_ratio",updateStrategy=FieldStrategy.NEVER,insertStrategy=FieldStrategy.NEVER)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var likeRatio: Double? = null,
    @TableField("vector", select = false,typeHandler = VectorTypeHandler::class)
    var vecotr: FloatArray? = null,
){
    companion object{
         val BASIC_COL= listOf("id","title","summary","status","is_top","is_recommend","view_count","like_count","comment_count","tags","user_id","published_at","kb_id","version","img","created_at","category_id","seo_keywords","seo_description")
    }

    fun needUpdate():Boolean
    {
        return  id!=null&&(title!=null ||  img!=null|| summary!=null || status!=null  || tags != null||top!=null)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PostEntity

        if (id != other.id) return false
        if (top != other.top) return false
        if (recommend != other.recommend) return false
        if (viewCount != other.viewCount) return false
        if (likeCount != other.likeCount) return false
        if (commentCount != other.commentCount) return false
        if (userId != other.userId) return false
        if (categoryId != other.categoryId) return false
        if (kbId != other.kbId) return false
        if (version != other.version) return false
        if (likeRatio != other.likeRatio) return false
        if (title != other.title) return false
        if (content != other.content) return false
        if (summary != other.summary) return false
        if (status != other.status) return false
        if (tags != other.tags) return false
        if (seoKeywords != other.seoKeywords) return false
        if (seoDescription != other.seoDescription) return false
        if (img != other.img) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (publishedAt != other.publishedAt) return false
        if (!vecotr.contentEquals(other.vecotr)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (top?.hashCode() ?: 0)
        result = 31 * result + (recommend?.hashCode() ?: 0)
        result = 31 * result + (viewCount ?: 0)
        result = 31 * result + (likeCount ?: 0)
        result = 31 * result + (commentCount ?: 0)
        result = 31 * result + (userId?.hashCode() ?: 0)
        result = 31 * result + (categoryId?.hashCode() ?: 0)
        result = 31 * result + (kbId?.hashCode() ?: 0)
        result = 31 * result + (version ?: 0)
        result = 31 * result + (likeRatio?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (content?.hashCode() ?: 0)
        result = 31 * result + (summary?.hashCode() ?: 0)
        result = 31 * result + (status?.hashCode() ?: 0)
        result = 31 * result + (tags?.hashCode() ?: 0)
        result = 31 * result + (seoKeywords?.hashCode() ?: 0)
        result = 31 * result + (seoDescription?.hashCode() ?: 0)
        result = 31 * result + (img?.hashCode() ?: 0)
        result = 31 * result + (createdAt?.hashCode() ?: 0)
        result = 31 * result + (updatedAt?.hashCode() ?: 0)
        result = 31 * result + (publishedAt?.hashCode() ?: 0)
        result = 31 * result + (vecotr?.contentHashCode() ?: 0)
        return result
    }
}