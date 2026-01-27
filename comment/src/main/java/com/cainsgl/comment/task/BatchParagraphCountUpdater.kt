package com.cainsgl.comment.task

import com.cainsgl.comment.repository.ParagraphMapper
import com.cainsgl.comment.repository.ParagraphUpdateDTO
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 批量更新段落计数的工具类
 * 累积到指定数量后批量更新数据库，减少数据库IO次数
 * 使用 CASE WHEN 实现真正的批量更新，一次SQL更新多条记录
 */
class BatchParagraphCountUpdater(
    private val paragraphMapper: ParagraphMapper,
    private val batchSize:Int
) {
    // 存储格式: Triple(postId, version, Map<dataId, count>)
    private val updateBatch = mutableListOf<Triple<Long, Int, Map<String, Long>>>()

    /**
     * 添加一条更新记录
     * 当累积到batchSize时自动触发批量更新
     */
    fun update(postId: Long, version: Int, data: Map<String, Long>) {
        updateBatch.add(Triple(postId, version, data))
        
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
            batchUpdateParagraphCount(updateBatch)
            logger.debug { "批量更新完成，共 ${updateBatch.size} 条记录" }
        } catch (e: Exception) {
            logger.error(e) { "批量更新失败" }
        } finally {
            updateBatch.clear()
        }
    }

    /**
     * 将批次数据转换为 DTO 列表并执行批量更新
     * 使用一条 SQL 语句完成所有更新操作
     */
    private fun batchUpdateParagraphCount(batch: List<Triple<Long, Int, Map<String, Long>>>) {
        if (batch.isEmpty()) return

        // 将所有更新展开为 DTO 列表
        val updates = batch.flatMap { (postId, version, dataMap) ->
            dataMap.map { (dataId, incrementValue) ->
                ParagraphUpdateDTO(
                    postId = postId,
                    version = version,
                    dataId = dataId.toInt(),
                    incrementValue = incrementValue
                )
            }
        }

        if (updates.isEmpty()) return

        try {
            val affectedRows = paragraphMapper.batchIncrementParagraphCount(updates)
            logger.debug { "批量更新段落计数成功，影响 $affectedRows 行，共 ${updates.size} 条更新" }
        } catch (e: Exception) {
            logger.error(e) { "批量更新段落计数失败，共 ${updates.size} 条更新" }
            throw e
        }
    }
}
