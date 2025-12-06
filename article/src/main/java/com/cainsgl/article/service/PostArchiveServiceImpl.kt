package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.article.PostArchiveEntity
import com.cainsgl.common.service.article.post.archive.PostArchiveService
import com.cainsgl.article.repository.PostArchiveMapper
import org.springframework.stereotype.Service

@Service
class PostArchiveServiceImpl : ServiceImpl<PostArchiveMapper, PostArchiveEntity>(), PostArchiveService, IService<PostArchiveEntity> {

    /**
     * 获取归档信息
     * @param id
     * @return
     */
    fun getPostArchive(id: Long): PostArchiveEntity? {
        return baseMapper.selectById(id)
    }

}
