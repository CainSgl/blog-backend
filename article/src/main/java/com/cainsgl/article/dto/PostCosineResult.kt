package com.cainsgl.article.dto

data class PostCosineResult(
    val postId: Long,
    val distance: Double,
    val chunk: String? = null
)
