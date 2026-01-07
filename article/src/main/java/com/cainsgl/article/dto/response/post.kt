package com.cainsgl.article.dto.response

import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.OperateType
import com.cainsgl.common.entity.article.PostEntity
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.springframework.beans.BeanUtils
import java.time.LocalDateTime

data class CreatePostResponse(
    val post: PostEntity,
    @JsonSerialize(using = ToStringSerializer::class)
    val dirId: Long
)
data class GetPostResponse(
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    var title: String? = null,
    var content: String ?= null,
    var summary: String? = null,
    var status: ArticleStatus? = null,
    var top: Boolean ?= null,
    var recommend: Boolean ?= null,
    var viewCount: Int? = null,
    var likeCount: Int? = null,
    var commentCount: Int? = null,
    var tags: List<String>? = null,
    @JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,
    @JsonSerialize(using = ToStringSerializer::class)
    var categoryId: Long? = null,
    var seoKeywords: String? = null,
    var seoDescription: String? = null,
    var img: String? = null,
    var createdAt: LocalDateTime?=null,
    var updatedAt: LocalDateTime?=null,
    var publishedAt: LocalDateTime? = null,
    @JsonSerialize(using = ToStringSerializer::class)
    var kbId: Long? = null,
    var operate:Set<OperateType>
){
    constructor(post: PostEntity,operate:Set<OperateType>):this(operate=operate)
    {
        BeanUtils.copyProperties(post, this)
    }

}