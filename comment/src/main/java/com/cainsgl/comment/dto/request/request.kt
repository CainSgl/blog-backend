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