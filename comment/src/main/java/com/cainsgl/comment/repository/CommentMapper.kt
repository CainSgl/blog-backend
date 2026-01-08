package com.cainsgl.comment.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.comment.entity.CommentEntity
import org.apache.ibatis.annotations.Mapper
import java.time.LocalDate

@Mapper
interface CommentMapper : BaseMapper<CommentEntity> {
    /**
     * 游标分页查询评论 - 根据postId, dataId, version以及游标参数进行查询
     */
    fun selectByCursor(postId: Long, version: Int, dataId: Int, lastCreatedAt: LocalDate?, lastLikeCount: Int?): List<CommentEntity>
}