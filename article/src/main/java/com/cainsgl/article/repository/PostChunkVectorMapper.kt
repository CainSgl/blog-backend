package com.cainsgl.article.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.article.dto.PostCosineResult
import com.cainsgl.common.entity.article.PostChunkVectorEntity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface PostChunkVectorMapper : BaseMapper<PostChunkVectorEntity> {

    /**
     * 通过向量计算L2距离（适用于归一化向量）
     * @param targetVector 目标向量
     * @param startValue 开始的值，最开始应该是0，查询下一页时根据上一页返回的最大距离值决定
     * @param limit 限制的数量
     * @return 距离越小越相似
     */
    fun selectPostsByCosine(
        @Param("targetVector") targetVector: FloatArray,
        @Param("startValue") startValue: Double,
        @Param("limit") limit: Int
    ): List<PostCosineResult>
}
