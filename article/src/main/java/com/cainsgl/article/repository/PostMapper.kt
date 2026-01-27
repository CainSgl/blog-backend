package com.cainsgl.article.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.article.PostEntity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDateTime

@Mapper
interface PostMapper : BaseMapper<PostEntity> {
    
    /**
     * 批量查询文章基础信息（排除content等大字段）
     */
    fun selectBasicInfoByIds(ids: List<Long>): List<PostEntity>

    fun selectVectorById(id:Long):FloatArray?
    
    /**
     * 游标分页查询文章列表
     */
    fun selectPostsByCursor(
        lastUpdatedAt: LocalDateTime,
        lastLikeRatio: Double,
        lastId: Long,
        pageSize: Int
    ): List<PostEntity>
    fun selectFirstPage(page:Int):List<PostEntity>

    /**
     * 根据指定post_id的向量查找最相似的文章
     */
    fun selectSimilarPostsByVector(postId: Long, limit: Int): List<PostEntity>
    fun selectPostsByVector(targetVector: FloatArray, limit: Int): List<PostEntity>

    /**
     * 批量增量更新文章计数
     * 使用 CASE WHEN 实现一次SQL更新多条记录
     */
    fun batchIncrementPostCount(
        @Param("postIds") postIds: List<Long>,
        @Param("viewCountMap") viewCountMap: Map<Long, Long>,
        @Param("commentCountMap") commentCountMap: Map<Long, Long>,
        @Param("likeCountMap") likeCountMap: Map<Long, Long>,
        @Param("starCountMap") starCountMap: Map<Long, Long>
    ): Int
}
