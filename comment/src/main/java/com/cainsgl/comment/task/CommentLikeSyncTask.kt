package com.cainsgl.comment.task

import com.cainsgl.comment.repository.ParCommentMapper
import com.cainsgl.comment.repository.PostsCommentMapper
import com.cainsgl.comment.repository.ReplyMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class CommentLikeSyncTask {

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Int>

    @Resource
    lateinit var postsCommentMapper: PostsCommentMapper

    @Resource
    lateinit var parCommentMapper: ParCommentMapper

    @Resource
    lateinit var replyMapper: ReplyMapper

    @Value("\${idle.task.batch-size:300}")
    private val batchSize: Int = 300

    @Value("\${idle.task.db-batch-size:30}")
    private val dbBatchSize: Int = 30

    companion object {
        const val POST_COMMENT_LIKE_PREFIX = "cursor:post_comment:like:"
        const val PAR_COMMENT_LIKE_PREFIX = "cursor:par_comment:like:"
        const val REPLY_LIKE_PREFIX = "cursor:reply:like:"
    }

    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    fun syncCommentLikesToDatabase() {
        logger.debug { "开始同步评论点赞数到数据库" }
        
        syncPostCommentLikes()
        syncParCommentLikes()
        syncReplyLikes()
        
        logger.debug { "评论点赞数同步完成" }
    }

    private fun syncPostCommentLikes() {
        try {
            val cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match("${POST_COMMENT_LIKE_PREFIX}*").count(batchSize.toLong()).build()
            )

            val batchUpdater = BatchCommentLikeUpdater(postsCommentMapper, parCommentMapper, replyMapper, dbBatchSize)
            var processedCount = 0

            cursor.use {
                while (cursor.hasNext() && processedCount < batchSize) {
                    val key = cursor.next()
                    try {
                        val value = redisTemplate.opsForValue().getAndDelete(key)
                        val incrementValue = value?: continue
                        if(incrementValue ==0) {
                            continue
                        }
                        val id = key.substringAfter(POST_COMMENT_LIKE_PREFIX).toLongOrNull() ?: continue
                        batchUpdater.updatePostComment(id, incrementValue)
                        redisTemplate.delete(key)
                        processedCount++
                    } catch (e: Exception) {
                        logger.error(e) { "处理文章评论点赞key失败: $key" }
                    }
                }
                batchUpdater.flush()
                if (processedCount > 0) {
                    logger.info { "同步文章评论点赞数完成，共处理 $processedCount 条" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "同步文章评论点赞数失败" }
        }
    }

    private fun syncParCommentLikes() {
        try {
            val cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match("${PAR_COMMENT_LIKE_PREFIX}*").count(batchSize.toLong()).build()
            )

            val batchUpdater = BatchCommentLikeUpdater(postsCommentMapper, parCommentMapper, replyMapper, dbBatchSize)
            var processedCount = 0

            cursor.use {
                while (cursor.hasNext() && processedCount < batchSize) {
                    val key = cursor.next()
                    try {
                        val value = redisTemplate.opsForValue().getAndDelete(key)
                        val incrementValue = value?: continue

                        if(incrementValue ==0) {
                            continue
                        }

                        val id = key.substringAfter(PAR_COMMENT_LIKE_PREFIX).toLongOrNull() ?: continue
                        batchUpdater.updateParComment(id, incrementValue)
                        redisTemplate.delete(key)
                        processedCount++
                    } catch (e: Exception) {
                        logger.error(e) { "处理段落评论点赞key失败: $key" }
                    }
                }
                batchUpdater.flush()
                if (processedCount > 0) {
                    logger.info { "同步段落评论点赞数完成，共处理 $processedCount 条" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "同步段落评论点赞数失败" }
        }
    }

    private fun syncReplyLikes() {
        try {
            val cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match("${REPLY_LIKE_PREFIX}*").count(batchSize.toLong()).build()
            )

            val batchUpdater = BatchCommentLikeUpdater(postsCommentMapper, parCommentMapper, replyMapper, dbBatchSize)
            var processedCount = 0

            cursor.use {
                while (cursor.hasNext() && processedCount < batchSize) {
                    val key = cursor.next()
                    try {
                        val value = redisTemplate.opsForValue().getAndDelete(key)
                        val incrementValue = value?: continue

                        if(incrementValue ==0) {
                            continue
                        }

                        val id = key.substringAfter(REPLY_LIKE_PREFIX).toLongOrNull() ?: continue
                        batchUpdater.updateReply(id, incrementValue)
                        redisTemplate.delete(key)
                        processedCount++
                    } catch (e: Exception) {
                        logger.error(e) { "处理回复点赞key失败: $key" }
                    }
                }
                batchUpdater.flush()
                if (processedCount > 0) {
                    logger.info { "同步回复点赞数完成，共处理 $processedCount 条" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "同步回复点赞数失败" }
        }
    }
}
