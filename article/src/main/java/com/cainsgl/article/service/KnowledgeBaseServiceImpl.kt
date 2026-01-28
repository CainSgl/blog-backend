package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.kb.KnowledgeBaseService
import com.cainsgl.article.repository.KnowledgeBaseMapper
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class KnowledgeBaseServiceImpl : ServiceImpl<KnowledgeBaseMapper, KnowledgeBaseEntity>(), KnowledgeBaseService, IService<KnowledgeBaseEntity> {

    override fun addKbLikeCount(kbId: Long, addCount: Int) {
        val wrapper = KtUpdateWrapper(KnowledgeBaseEntity::class.java)
        wrapper.eq(KnowledgeBaseEntity::id, kbId)
        wrapper.setSql("like_count = like_count + $addCount")
        baseMapper.update(wrapper)
    }

    override fun getByIds(ids: List<Long>): List<KnowledgeBaseEntity> {
        if (ids.isEmpty()) return emptyList()
        return listByIds(ids)
    }

    fun cursor(lastCreatedAt: LocalDateTime?, lastLike: Int?, lastId: Long?, pageSize: Int): List<KnowledgeBaseEntity>
    {
        if (lastCreatedAt == null || lastLike == null || lastId == null)
        {
            return baseMapper.selectFirstPage(pageSize)
        }
        return baseMapper.selectKbByCursor(lastCreatedAt, lastLike, lastId, pageSize)
    }


}
