package com.cainsgl.api.article.post.history

import com.cainsgl.common.entity.article.PostHistoryEntity

interface PostHistoryService {
    fun getLastById(postId: Long): PostHistoryEntity?
}