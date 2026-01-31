package com.cainsgl.comment.task


import com.cainsgl.comment.repository.ParCommentMapper
import com.cainsgl.comment.repository.PostsCommentMapper
import com.cainsgl.comment.repository.ReplyMapper
import io.github.oshai.kotlinlogging.KotlinLogging
data class CommentLikeUpdateDTO(
    val id: Long,
    val incrementValue: Int
)
private val logger = KotlinLogging.logger {}

/**
 * 批量更新评论点赞数的工具类
 * 累积到指定数量后批量更新数据库，减少数据库IO次数
 */
class BatchCommentLikeUpdater(
    private val postsCommentMapper: PostsCommentMapper,
    private val parCommentMapper: ParCommentMapper,
    private val replyMapper: ReplyMapper,
    private val batchSize: Int
) {
    private val postCommentBatch = mutableListOf<CommentLikeUpdateDTO>()
    private val parCommentBatch = mutableListOf<CommentLikeUpdateDTO>()
    private val replyBatch = mutableListOf<CommentLikeUpdateDTO>()

    /**
     * 添加文章评论点赞更新
     */
    fun updatePostComment(id: Long, incrementValue: Int) {
        postCommentBatch.add(CommentLikeUpdateDTO(id, incrementValue))
        if (postCommentBatch.size >= batchSize) {
            flushPostComment()
        }
    }

    /**
     * 添加段落评论点赞更新
     */
    fun updateParComment(id: Long, incrementValue: Int) {
        parCommentBatch.add(CommentLikeUpdateDTO(id, incrementValue))
        if (parCommentBatch.size >= batchSize) {
            flushParComment()
        }
    }

    /**
     * 添加回复点赞更新
     */
    fun updateReply(id: Long, incrementValue: Int) {
        replyBatch.add(CommentLikeUpdateDTO(id, incrementValue))
        if (replyBatch.size >= batchSize) {
            flushReply()
        }
    }

    /**
     * 刷新所有待更新的数据到数据库
     */
    fun flush() {
        flushPostComment()
        flushParComment()
        flushReply()
    }

    private fun flushPostComment() {
        if (postCommentBatch.isEmpty()) return
        try {
            val affectedRows = postsCommentMapper.batchIncrementLikeCount(postCommentBatch)
            logger.debug { "批量更新文章评论点赞数完成，影响 $affectedRows 行" }
        } catch (e: Exception) {
            logger.error(e) { "批量更新文章评论点赞数失败" }
        } finally {
            postCommentBatch.clear()
        }
    }

    private fun flushParComment() {
        if (parCommentBatch.isEmpty()) return
        try {
            val affectedRows = parCommentMapper.batchIncrementLikeCount(parCommentBatch)
            logger.debug { "批量更新段落评论点赞数完成，影响 $affectedRows 行" }
        } catch (e: Exception) {
            logger.error(e) { "批量更新段落评论点赞数失败" }
        } finally {
            parCommentBatch.clear()
        }
    }

    private fun flushReply() {
        if (replyBatch.isEmpty()) return
        try {
            val affectedRows = replyMapper.batchIncrementLikeCount(replyBatch)
            logger.debug { "批量更新回复点赞数完成，影响 $affectedRows 行" }
        } catch (e: Exception) {
            logger.error(e) { "批量更新回复点赞数失败" }
        } finally {
            replyBatch.clear()
        }
    }
}
