package com.cainsgl.article.dto

import com.cainsgl.common.entity.article.PostEntity

/**
 * 单个文本块的得分结果
 */
data class ChunkScore(
    val chunk: String,
    val score: Double
)

/**
 * 文章及其相关文本块的聚合结果
 * @param postId 文章ID
 * @param aggregatedScore 聚合得分（用于排序，越小越相似）
 * @param chunks 该文章匹配的所有文本块及其得分
 */
data class PostChunkScoreResult(
    val article:PostEntity,
    val aggregatedScore: Double,
    val chunks: List<ChunkScore>
)
