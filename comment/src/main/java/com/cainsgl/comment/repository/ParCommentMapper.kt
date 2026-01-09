package com.cainsgl.comment.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.comment.entity.ParCommentEntity
import org.apache.ibatis.annotations.Mapper
import java.time.LocalDateTime

@Mapper
interface ParCommentMapper : BaseMapper<ParCommentEntity> {
    /**
     * 游标分页查询评论 - 根据postId, dataId, version以及游标参数进行查询
     */
    fun selectByCursor(postId: Long, version: Int, dataId: Int, lastCreatedAt: LocalDateTime?, lastLikeCount: Int?,lastId:Long?): List<ParCommentEntity>
}