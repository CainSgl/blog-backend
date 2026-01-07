package com.cainsgl.article.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.article.PostHistoryEntity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface PostHistoryMapper : BaseMapper<PostHistoryEntity> {
    @Select("""
        SELECT ph.content
        FROM posts_history ph
        WHERE ph.id = #{id} 
          AND ph.post_id = #{postId}
          AND ph.version < (SELECT MAX(version) FROM posts_history WHERE post_id = #{postId})
    """)
    fun getContentByIdAndPostIdWithNonMaxVersion(
        @Param("id") id: Long,
        @Param("postId") postId: Long
    ): String?
}