package com.cainsgl.article.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.article.PostChunkVectorEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface PostChunkVectorMapper : BaseMapper<PostChunkVectorEntity> {

}
