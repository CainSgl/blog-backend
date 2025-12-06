package com.cainsgl.article.service

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.article.dto.request.kb.UpdateKnowledgeBaseRequest
import com.cainsgl.article.repository.KnowledgeBaseMapper
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import com.cainsgl.common.service.article.kb.KnowledgeBaseService
import org.springframework.stereotype.Service

@Service
class KnowledgeBaseServiceImpl : ServiceImpl<KnowledgeBaseMapper, KnowledgeBaseEntity>(), KnowledgeBaseService, IService<KnowledgeBaseEntity> {

    fun updateKnowledgeBase(request: UpdateKnowledgeBaseRequest): Boolean {
        val updateWrapper = UpdateWrapper<KnowledgeBaseEntity>()
        updateWrapper.eq("id", request.id)
            .set("name", request.name)
            .set("status", request.status)
        return update(updateWrapper)
    }
}
