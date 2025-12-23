package com.cainsgl.article.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.post.history.PostHistoryService
import com.cainsgl.article.repository.PostHistoryMapper
import com.cainsgl.common.entity.article.PostHistoryEntity
import org.springframework.stereotype.Service

@Service
class PostHistoryServiceImpl : ServiceImpl<PostHistoryMapper, PostHistoryEntity>(), PostHistoryService, IService<PostHistoryEntity> {
    /**
     * 实际上是返回倒数第二个，因为最新版本是给作者缓存用的
     */
    override fun getLastById(postId: Long): PostHistoryEntity?
    {
        val historyQuery = QueryWrapper<PostHistoryEntity>()
            .eq("post_id", postId) .orderByDesc("version").last("LIMIT 1 OFFSET 1")
        return baseMapper.selectOne(historyQuery)
    }

}