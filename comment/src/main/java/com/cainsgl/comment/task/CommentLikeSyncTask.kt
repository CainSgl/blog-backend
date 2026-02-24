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

    // 两阶段删除：先移动到备份key，处理成功后再删除备份
    private val getAndMoveScript = """
        local value = redis.call('GET', KEYS[1])
        if value then
            redis.call('RENAME', KEYS[1], KEYS[2])
            redis.call('EXPIRE', KEYS[2], 3600)
        end
        return value
    """.trimIndent()

    private val deleteBackupScript = """
        redis.call('DEL', KEYS[1])
        return 1
    """.trimIndent()

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
            val backupKeys = mutableListOf<String>()
            var processedCount = 0

            cursor.use {
                while (cursor.hasNext() && processedCount < batchSize) {
                    val key = cursor.next()
                    try {
                        // 生成备份key
                        val backupKey = "${key}:backup"
                        
                        // 使用Lua脚本原子性地获取并移动到备份key
                        val incrementValue = getAndMoveToBackup(key, backupKey) ?: continue
                        if (incrementValue == 0) continue
                        
                        val id = key.substringAfter(POST_COMMENT_LIKE_PREFIX).toLongOrNull() ?: continue
                        batchUpdater.updatePostComment(id, incrementValue)
                        
                        // 记录备份key，待数据库更新成功后删除
                        backupKeys.add(backupKey)
                        processedCount++
                    } catch (e: Exception) {
                        logger.error(e) { "处理文章评论点赞key失败: $key" }
                    }
                }
                
                // 刷新残留数据到数据库
                batchUpdater.flush()
                
                // 数据库更新成功后，删除所有备份key
                deleteBackupKeys(backupKeys)
                
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
            val backupKeys = mutableListOf<String>()
            var processedCount = 0

            cursor.use {
                while (cursor.hasNext() && processedCount < batchSize) {
                    val key = cursor.next()
                    try {
                        // 生成备份key
                        val backupKey = "${key}:backup"
                        
                        // 使用Lua脚本原子性地获取并移动到备份key
                        val incrementValue = getAndMoveToBackup(key, backupKey) ?: continue
                        if (incrementValue == 0) continue

                        val id = key.substringAfter(PAR_COMMENT_LIKE_PREFIX).toLongOrNull() ?: continue
                        batchUpdater.updateParComment(id, incrementValue)
                        
                        // 记录备份key，待数据库更新成功后删除
                        backupKeys.add(backupKey)
                        processedCount++
                    } catch (e: Exception) {
                        logger.error(e) { "处理段落评论点赞key失败: $key" }
                    }
                }
                
                // 刷新残留数据到数据库
                batchUpdater.flush()
                
                // 数据库更新成功后，删除所有备份key
                deleteBackupKeys(backupKeys)
                
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
            val backupKeys = mutableListOf<String>()
            var processedCount = 0

            cursor.use {
                while (cursor.hasNext() && processedCount < batchSize) {
                    val key = cursor.next()
                    try {
                        // 生成备份key
                        val backupKey = "${key}:backup"
                        
                        // 使用Lua脚本原子性地获取并移动到备份key
                        val incrementValue = getAndMoveToBackup(key, backupKey) ?: continue
                        if (incrementValue == 0) continue

                        val id = key.substringAfter(REPLY_LIKE_PREFIX).toLongOrNull() ?: continue
                        batchUpdater.updateReply(id, incrementValue)
                        
                        // 记录备份key，待数据库更新成功后删除
                        backupKeys.add(backupKey)
                        processedCount++
                    } catch (e: Exception) {
                        logger.error(e) { "处理回复点赞key失败: $key" }
                    }
                }
                
                // 刷新残留数据到数据库
                batchUpdater.flush()
                
                // 数据库更新成功后，删除所有备份key
                deleteBackupKeys(backupKeys)
                
                if (processedCount > 0) {
                    logger.info { "同步回复点赞数完成，共处理 $processedCount 条" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "同步回复点赞数失败" }
        }
    }

    private fun getAndMoveToBackup(key: String, backupKey: String): Int? {
        return try {
            val result = redisTemplate.execute<Any> { connection ->
                val keyBytes = key.toByteArray()
                val backupKeyBytes = backupKey.toByteArray()
                val scriptBytes = getAndMoveScript.toByteArray()
                connection.eval(scriptBytes, org.springframework.data.redis.connection.ReturnType.VALUE, 2, keyBytes, backupKeyBytes)
            }
            
            when (result) {
                is ByteArray -> String(result).toIntOrNull()
                is String -> result.toIntOrNull()
                is Int -> result
                else -> null
            }
        } catch (e: Exception) {
            logger.error(e) { "执行Lua脚本失败: $key" }
            null
        }
    }

    private fun deleteBackupKeys(backupKeys: List<String>) {
        if (backupKeys.isEmpty()) return
        
        try {
            backupKeys.forEach { backupKey ->
                try {
                    redisTemplate.execute<Any> { connection ->
                        val keyBytes = backupKey.toByteArray()
                        val scriptBytes = deleteBackupScript.toByteArray()
                        connection.eval(scriptBytes, org.springframework.data.redis.connection.ReturnType.INTEGER, 1, keyBytes)
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "删除备份key失败: $backupKey (将在1小时后自动过期)" }
                }
            }
            logger.debug { "成功删除 ${backupKeys.size} 个备份key" }
        } catch (e: Exception) {
            logger.error(e) { "批量删除备份key失败" }
        }
    }
}
