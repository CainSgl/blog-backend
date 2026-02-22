package com.cainsgl.file.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * 基于Redis Sorted Set 的延迟任务队列
 */
@Component
class DelayedTaskQueue(
    private val redisTemplate: StringRedisTemplate
) {
    companion object {
        private const val QUEUE_KEY = "delayed_task:file_verification"
    }


    fun addTask(shortUrl: Long, delaySeconds: Long) {
        try {
            val executeTime = Instant.now().epochSecond + delaySeconds
            redisTemplate.opsForZSet().add(QUEUE_KEY, shortUrl.toString(), executeTime.toDouble())
        } catch (e: Exception) {
            logger.error(e) { "添加文件验证任务失败: shortUrl=$shortUrl" }
        }
    }


    fun pollDueTasks(batchSize: Int = 100): List<Long> {
        try {
            val now = Instant.now().epochSecond.toDouble()
            val tasks = redisTemplate.opsForZSet()
                .rangeByScore(QUEUE_KEY, 0.0, now, 0, batchSize.toLong())
                ?: emptySet()
            
            return tasks.mapNotNull { it.toLongOrNull() }
        } catch (e: Exception) {
            logger.error(e) { "获取到期任务失败" }
            return emptyList()
        }
    }


    fun removeTask(shortUrl: Long) {
        try {
            redisTemplate.opsForZSet().remove(QUEUE_KEY, shortUrl.toString())
        } catch (e: Exception) {
            logger.error(e) { "删除任务失败: shortUrl=$shortUrl" }
        }
    }


    fun removeTasks(shortUrls: Collection<Long>) {
        if (shortUrls.isEmpty()) return
        try {
            val shortUrlStrs = shortUrls.map { it.toString() }.toTypedArray()
            redisTemplate.opsForZSet().remove(QUEUE_KEY, *shortUrlStrs)
        } catch (e: Exception) {
            logger.error(e) { "批量删除任务失败" }
        }
    }


    fun getQueueSize(): Long {
        return redisTemplate.opsForZSet().size(QUEUE_KEY) ?: 0
    }
}
