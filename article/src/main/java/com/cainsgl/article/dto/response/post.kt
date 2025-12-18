package com.cainsgl.article.dto.response

import com.cainsgl.common.entity.article.PostEntity
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

data class CreatePostResponse(
    val post: PostEntity,
    @JsonSerialize(using = ToStringSerializer::class)
    val dirId: Long
)
