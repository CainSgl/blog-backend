package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.common.service.article.PostService
import com.cainsgl.article.repository.PostMapper
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

}
