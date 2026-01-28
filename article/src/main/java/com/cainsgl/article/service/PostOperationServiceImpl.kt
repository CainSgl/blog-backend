package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.post.operation.PostOperationService
import com.cainsgl.article.repository.PostOperationMapper
import com.cainsgl.common.entity.article.OperateType
import com.cainsgl.common.entity.article.PostOperationEntity
import org.springframework.stereotype.Service

@Service
class PostOperationServiceImpl : ServiceImpl<PostOperationMapper, PostOperationEntity>(), PostOperationService,
    IService<PostOperationEntity>
{
    fun getOperateByUserIdAndPostId(userId: Long, postId: Long): Set<OperateType>
    {
        val set: MutableSet<OperateType> = mutableSetOf()
        getListByUserIdAndPostId(userId, postId).forEach { e ->
            set.add(OperateType.getByValue(e.operateType))
        }
        return set
    }

    fun getListByUserIdAndPostId(userId: Long, postId: Long): List<PostOperationEntity>
    {
        val query = KtQueryWrapper(PostOperationEntity::class.java)
        query.eq(PostOperationEntity::userId, userId).eq(PostOperationEntity::targetId, postId)
        return baseMapper.selectList(query)
    }

    fun hasOperate(userId: Long, targetId: Long, type: OperateType): Boolean
    {
        val query =KtQueryWrapper(PostOperationEntity::class.java)
            .eq(PostOperationEntity::userId, userId).eq(PostOperationEntity::targetId, targetId).eq(PostOperationEntity::operateType, type.value)

        return baseMapper.selectCount(query) > 0
    }

    fun addOperate(id: Long, type: OperateType, add: Boolean, userId: Long)
    {
        if (add)
        {
            this.save(PostOperationEntity(userId = userId, targetId = id, operateType = type.value))
        } else
        {
            val query = KtUpdateWrapper(PostOperationEntity::class.java)
                .eq(PostOperationEntity::userId, userId)
                .eq(PostOperationEntity::targetId, id)
                .eq(PostOperationEntity::operateType, type.value)

            this.remove(query)
        }
    }
}