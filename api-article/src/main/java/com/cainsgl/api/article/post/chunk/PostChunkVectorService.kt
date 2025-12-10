package com.cainsgl.api.article.post.chunk

interface PostChunkVectorService {
    fun reloadVector(postId: Long,originContent: String?): Boolean
    fun removeVector(postId: Long): Boolean
}
