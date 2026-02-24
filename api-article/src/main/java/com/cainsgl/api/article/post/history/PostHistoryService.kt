package com.cainsgl.api.article.post.history

import com.cainsgl.common.entity.article.PostHistoryEntity

interface PostHistoryService {
    fun getLastById(postId: Long): PostHistoryEntity?
    fun updateById(historyId: Long, content: String): Boolean
    fun createNewVersion(userId: Long, postId: Long, version: Int, content: String): Boolean
}