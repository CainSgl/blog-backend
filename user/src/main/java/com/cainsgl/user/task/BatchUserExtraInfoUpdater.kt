package com.cainsgl.user.task

import com.cainsgl.user.repository.UserExtraInfoMapper
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 批量更新用户额外信息的工具类
 * 累积到指定数量后批量更新数据库，减少数据库IO次数
 */
class BatchUserExtraInfoUpdater(
    private val userExtraInfoMapper: UserExtraInfoMapper,
    private val batchSize: Int = 10
) {
    private val updateBatch = mutableListOf<Pair<Long, Map<String, Long>>>()

    /**
     * 添加一条更新记录
     * 当累积到batchSize时自动触发批量更新
     */
    fun update(userId: Long, data: Map<String, Long>) {
        updateBatch.add(userId to data)
        
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
            batchUpdateUserExtraInfo(updateBatch)
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
    private fun batchUpdateUserExtraInfo(batch: List<Pair<Long, Map<String, Long>>>) {
        if (batch.isEmpty()) return

        val userIds = batch.map { it.first }
        
        // 构建批量更新的数据结构
        val likeCountMap = mutableMapOf<Long, Long>()
        val commentCountMap = mutableMapOf<Long, Long>()
        val postCountMap = mutableMapOf<Long, Long>()
        val articleViewCountMap = mutableMapOf<Long, Long>()
        val followerCountMap = mutableMapOf<Long, Long>()
        val followingCountMap = mutableMapOf<Long, Long>()
        val msgCountMap = mutableMapOf<Long, Long>()
        val msgReplyCountMap = mutableMapOf<Long, Long>()
        val msgLikeCountMap=mutableMapOf<Long, Long>()
        val msgReportCountMap=mutableMapOf<Long, Long>()
        val msgMessageCountMap = mutableMapOf<Long, Long>()

        batch.forEach { (userId, data) ->
            data.forEach { (field, value) ->
                when (field) {
                    "likeCount" -> likeCountMap[userId] = value
                    "commentCount" -> commentCountMap[userId] = value
                    "postCount" -> postCountMap[userId] = value
                    "articleViewCount" -> articleViewCountMap[userId] = value
                    "followerCount" -> followerCountMap[userId] = value
                    "followingCount" -> followingCountMap[userId] = value
                    "msgCount"->msgCountMap[userId] = value
                    "msgReplyCount"->msgReplyCountMap[userId] = value
                    "msgLikeCount"->msgLikeCountMap[userId] = value
                    "msgReportCount"->msgReportCountMap[userId] = value
                    "msgMessageCount"->msgMessageCountMap[userId] = value
                }
            }
        }

        // 调用Mapper的批量更新方法
        userExtraInfoMapper.batchIncrementUserExtraInfo(
            userIds,
            likeCountMap,
            commentCountMap,
            postCountMap,
            articleViewCountMap,
            followerCountMap,
            followingCountMap,
            msgCountMap,
            msgReplyCountMap,
            msgLikeCountMap,
            msgReportCountMap,
            msgMessageCountMap
        )
    }
}
