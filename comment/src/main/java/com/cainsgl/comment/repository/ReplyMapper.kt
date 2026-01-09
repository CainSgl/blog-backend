package com.cainsgl.comment.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.comment.entity.ReplyEntity
import org.apache.ibatis.annotations.Mapper
import java.time.LocalDate

@Mapper
interface ReplyMapper : BaseMapper<ReplyEntity> {
    /**
     * 通过父评论ID进行游标分页查询回复
     */
    fun selectByParCommentIdCursor(parCommentId: Long, lastCreatedAt: LocalDate?, lastLikeCount: Int?,lastId:Long?): List<ReplyEntity>

    /**
     * 通过帖子评论ID进行游标分页查询回复
     */
    fun selectByPostCommentIdCursor(postCommentId: Long, lastCreatedAt: LocalDate?, lastLikeCount: Int?,lastId:Long?): List<ReplyEntity>
}