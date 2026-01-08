package com.cainsgl.article.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.article.PostEntity
import org.apache.ibatis.annotations.Mapper
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

}
