package com.cainsgl.article.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.toolkit.IdWorker
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.article.dto.response.CursorResult
import com.cainsgl.article.dto.response.PostViewHistoryDTO
import com.cainsgl.article.entity.PostViewHistoryEntity
import com.cainsgl.article.repository.PostViewHistoryMapper
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PostViewHistoryServiceImpl : ServiceImpl<PostViewHistoryMapper, PostViewHistoryEntity>()
{
    fun createHistory(postId: Long, userId: Long)
    {
        val history = PostViewHistoryEntity(
            userId = userId,
            postId = postId,
            browseTime = LocalDateTime.now()
        )
        history.id = IdWorker.getId()
        baseMapper.saveOrUpdate(history)
    }

    fun getHistory(userId: Long, after: LocalDateTime?, limit: Int): CursorResult<PostViewHistoryDTO> {
        val list = baseMapper.selectHistoryWithPost(userId, after, limit)
        val nextAfter = if (list.isNotEmpty()) list.last().browseTime else null
        return CursorResult(list, nextAfter)
    }
}
