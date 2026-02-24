package com.cainsgl.article.dto

/**
 * 文章发布MQ消息
 */
data class PostPublishMessage(
    val postId: Long,
    val historyId: Long,
    val userId: Long,
    val version: Int,
    val content: String,
    val title: String,
    val summary: String?,
    val img: String?,
    val tags: List<String>?
)
