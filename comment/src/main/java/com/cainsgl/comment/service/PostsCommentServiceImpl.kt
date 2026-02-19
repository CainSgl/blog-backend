package com.cainsgl.comment.service

import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.comment.entity.PostsCommentEntity
import com.cainsgl.comment.repository.PostsCommentMapper
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PostsCommentServiceImpl : ServiceImpl<PostsCommentMapper, PostsCommentEntity>() {
    fun getByCursor(
        postId: Long,
        lastCreatedAt: LocalDateTime?,
        lastLikeCount: Int?,
        lastId:Long?
    ): List<PostsCommentEntity>
    {
        return baseMapper.selectByCursor(postId,  lastCreatedAt, lastLikeCount,lastId)
    }
    fun addReplyCount(id: Long, commentsCount: Int) :Boolean{
        val update= KtUpdateWrapper(PostsCommentEntity::class.java).eq(PostsCommentEntity::id,id).setSql("reply_count = reply_count + $commentsCount")
         return update(update)
    }
}