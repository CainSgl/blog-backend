package com.cainsgl.comment.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty

data class CreateParagraphRequest(
    @field:Min(value = 0, message = "id非法")
    val postId: Long,
    @field:Min(value = 0)
    val version: Int,
    @field:Min(value = 0)
    val dataId: Int,
    @field:NotEmpty
    val content:String,
)
data class CreateReplyRequest(
    val postCommentId: Long?,
    val parCommentId:Long?,
    val dataId:Int?,
    val replyId:Long,
    @field:NotEmpty
    val content:String,
    val postId:Long,
    val version:Int,
    val replyCommentId:Long?,
)
data class CommentPostRequest(
    @field:NotEmpty
    val content: String,
    var version: Int,
    @field:Min(0)
    val postId: Long,
)
