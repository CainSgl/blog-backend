package com.cainsgl.article.task

import com.cainsgl.article.repository.PostMapper
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 批量更新文章计数的工具类
 * 累积到指定数量后批量更新数据库，减少数据库IO次数
 */
class BatchPostCountUpdater(
    private val postMapper: PostMapper,
    private val batchSize: Int = 10
) {
    private val updateBatch = mutableListOf<Triple<Long, String, Long>>()

    /**
     * 添加一条更新记录
     * 当累积到batchSize时自动触发批量更新
     */
    fun update(postId: Long, type: String, count: Long) {
        updateBatch.add(Triple(postId, type, count))

        if (updateBatch.size >= batchSize) {
            flush()
        }
    }

    /**
     * 刷新所有待更新的数据到数据库
     * 在循环结束时调用，确保残留数据被写入
     */
    fun flush() {
        if (updateBatch.isEmpty()) return

        try {
            batchUpdatePostCount(updateBatch)
            logger.debug { "批量更新完成，共 ${updateBatch.size} 条记录" }
        } catch (e: Exception) {
            logger.error(e) { "批量更新失败" }
        } finally {
            updateBatch.clear()
        }
    }

    /**
     * 真正的批量更新：一次SQL更新多条记录
     * 使用 CASE WHEN 语句实现批量增量更新
     */
    private fun batchUpdatePostCount(batch: List<Triple<Long, String, Long>>) {
        if (batch.isEmpty()) return

        val postIds = batch.map { it.first }.distinct()

        // 构建批量更新的数据结构
        val viewCountMap = mutableMapOf<Long, Long>()
        val commentCountMap = mutableMapOf<Long, Long>()
        val likeCountMap = mutableMapOf<Long, Long>()
        val starCountMap = mutableMapOf<Long, Long>()

        batch.forEach { (postId, type, count) ->
            when (type) {
                "view" -> viewCountMap[postId] = (viewCountMap[postId] ?: 0) + count
                "comment" -> commentCountMap[postId] = (commentCountMap[postId] ?: 0) + count
                "like" -> likeCountMap[postId] = (likeCountMap[postId] ?: 0) + count
                "star" -> starCountMap[postId] = (starCountMap[postId] ?: 0) + count
            }
        }

        // 调用Mapper的批量更新方法
        postMapper.batchIncrementPostCount(
            postIds,
            viewCountMap,
            commentCountMap,
            likeCountMap,
            starCountMap
        )
    }
}
