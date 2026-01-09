package com.cainsgl.comment.service

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.comment.entity.ParCommentEntity
import com.cainsgl.comment.repository.ParCommentMapper
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ParCommentServiceImpl : ServiceImpl<ParCommentMapper, ParCommentEntity>()
{
    fun getByCursor(
        postId: Long,
        version: Int,
        dataId: Int,
        lastCreatedAt: LocalDateTime?,
        lastLikeCount: Int?,
        lastId:Long?
    ): List<ParCommentEntity>
    {
        return baseMapper.selectByCursor(postId, version, dataId, lastCreatedAt, lastLikeCount,lastId)
    }

    fun addReplyCount(id: Long, count: Int): Boolean
    {
        val wrapper = UpdateWrapper<ParCommentEntity>()
        wrapper.eq("id", id)
        wrapper.setSql("reply_count = reply_count + $count")
        return baseMapper.update(wrapper) > 0
    }

}