package com.cainsgl.comment.dto.request

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import java.time.LocalDate
import java.time.LocalDateTime

data class NoticeReplyResponse(
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @field:JsonSerialize(using = ToStringSerializer::class)
    var parCommentId: Long? = null,
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,
    var content: String? = null,
    var likeCount: Int? = null,
    var createdAt: LocalDateTime? = null,
    @field:JsonSerialize(using = ToStringSerializer::class)
    var postCommentId: Long? = null,
    @field:JsonSerialize(using = ToStringSerializer::class)
    var replyId: Long? = null,
    @field:JsonSerialize(using = ToStringSerializer::class)
    var replyCommentId: Long? = null,
    var because: String? = null,
)