package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.article.PostChunkVectorEntity
import com.cainsgl.common.service.article.PostChunkVectorService
import com.cainsgl.article.repository.PostChunkVectorMapper
import org.springframework.stereotype.Service

@Service
class PostChunkVectorServiceImpl : ServiceImpl<PostChunkVectorMapper, PostChunkVectorEntity>(), PostChunkVectorService, IService<PostChunkVectorEntity> {

    /**
     * 获取向量信息
     * @param id
     * @return
     */
    fun getPostChunkVector(id: Long): PostChunkVectorEntity? {
        return baseMapper.selectById(id)
    }

}
