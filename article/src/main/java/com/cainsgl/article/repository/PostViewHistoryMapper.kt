package com.cainsgl.article.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.article.dto.response.PostViewHistoryDTO
import com.cainsgl.article.entity.PostViewHistoryEntity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDateTime

@Mapper
interface PostViewHistoryMapper : BaseMapper<PostViewHistoryEntity> {
    
    fun saveOrUpdate(entity: PostViewHistoryEntity)

    fun selectHistoryWithPost(
        @Param("userId") userId: Long, 
        @Param("after") after: LocalDateTime?, 
        @Param("limit") limit: Int
    ): List<PostViewHistoryDTO>
}
