package com.cainsgl.comment.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.comment.entity.PostsCommentEntity
import com.cainsgl.comment.task.CommentLikeUpdateDTO
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDateTime

@Mapper
interface PostsCommentMapper : BaseMapper<PostsCommentEntity> {
    /**
     * 游标分页查询文章评论 - 根据postId, version以及游标参数进行查询
     */
    fun selectByCursor(postId: Long, lastCreatedAt: LocalDateTime?, lastLikeCount: Int?,lastId:Long?): List<PostsCommentEntity>

    /**
     * 批量增量更新文章评论点赞数
     */
    fun batchIncrementLikeCount(@Param("updates") updates: List<CommentLikeUpdateDTO>): Int
}

