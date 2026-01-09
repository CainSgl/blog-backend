package com.cainsgl.comment.service

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.comment.entity.ReplyEntity
import com.cainsgl.comment.repository.ReplyMapper
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReplyServiceImpl : ServiceImpl<ReplyMapper, ReplyEntity>() {
    @Resource
    private lateinit var replyMapper: ReplyMapper

     fun getByParCommentIdCursor(parCommentId: Long, lastCreatedAt: LocalDate?, lastLikeCount: Int?,lastId:Long?): List<ReplyEntity> {
        return replyMapper.selectByParCommentIdCursor(parCommentId, lastCreatedAt, lastLikeCount,lastId)
    }

     fun getByPostCommentIdCursor(postCommentId: Long, lastCreatedAt: LocalDate?, lastLikeCount: Int?,lastId:Long?): List<ReplyEntity> {
        return replyMapper.selectByPostCommentIdCursor(postCommentId, lastCreatedAt, lastLikeCount,lastId)
    }
}