package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.post.PostHistoryService
import com.cainsgl.article.repository.PostHistoryMapper
import com.cainsgl.common.entity.article.PostHistoryEntity
import org.springframework.stereotype.Service

@Service
class PostHistoryServiceImpl : ServiceImpl<PostHistoryMapper, PostHistoryEntity>(), PostHistoryService, IService<PostHistoryEntity> {

}