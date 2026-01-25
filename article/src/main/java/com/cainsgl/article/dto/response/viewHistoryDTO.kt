package com.cainsgl.article.dto.response

import com.cainsgl.common.entity.article.ArticleStatus
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDateTime

data class PostViewHistoryDTO(
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,

    @field:JsonSerialize(using = ToStringSerializer::class)
    var postId: Long? = null,

    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    var title: String? = null,
    var img: String? = null,
    var likeCount: Int? = null,
    var browseTime: LocalDateTime? = null,
    var count:Int? = null,
    var starCount:Int? = null,
    var status: ArticleStatus? = null,
    var tags:List<String>? = null,
    var publishedAt: LocalDateTime? = null,
    var viewCount:Int? = null,
    var summary:String? = null,
)

data class CursorResult<T>(
    val items: List<T>,
    val after: LocalDateTime? = null
)
