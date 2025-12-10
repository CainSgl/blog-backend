package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.post.PostService
import com.cainsgl.article.repository.PostMapper
import com.cainsgl.common.entity.article.PostEntity
import org.springframework.stereotype.Service

@Service
class PostServiceImpl : ServiceImpl<PostMapper, PostEntity>(), PostService, IService<PostEntity> {

    /**
     * 获取文章信息
     * @param id
     * @return
     */
    fun getPost(id: Long): PostEntity? {
        return baseMapper.selectById(id)
    }

    override fun getById(id: Long): PostEntity?
    {
        return baseMapper.selectById(id)
    }

}
