package com.cainsgl.comment.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.comment.entity.ParagraphEntity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface ParagraphMapper : BaseMapper<ParagraphEntity> {

    /**
     * 批量增量更新段落计数
     * 使用 CASE WHEN 实现一次SQL更新多条记录
     */
    fun batchIncrementParagraphCount(
        @Param("updates") updates: List<ParagraphUpdateDTO>
    ): Int
}

/**
 * 段落更新DTO
 * 用于批量更新时传递参数
 */
data class ParagraphUpdateDTO(
    val postId: Long,
    val version: Int,
    val dataId: Int,
    val incrementValue: Long
)