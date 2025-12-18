package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.kb.KnowledgeBaseService
import com.cainsgl.article.repository.KnowledgeBaseMapper
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import org.springframework.stereotype.Service

@Service
class KnowledgeBaseServiceImpl : ServiceImpl<KnowledgeBaseMapper, KnowledgeBaseEntity>(), KnowledgeBaseService, IService<KnowledgeBaseEntity> {

}
