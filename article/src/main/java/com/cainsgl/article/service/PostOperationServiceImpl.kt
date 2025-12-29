package com.cainsgl.article.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.post.operation.PostOperationService
import com.cainsgl.article.repository.PostOperationMapper
import com.cainsgl.common.entity.article.OperateType
import com.cainsgl.common.entity.article.PostOperationEntity
import org.springframework.stereotype.Service

@Service
class PostOperationServiceImpl : ServiceImpl<PostOperationMapper, PostOperationEntity>(), PostOperationService, IService<PostOperationEntity> {
    fun getOperateByUserIdAndPostId(userId: Long, postId: Long): Set<OperateType> {
        val set:MutableSet<OperateType> = mutableSetOf()
        getListByUserIdAndPostId(userId, postId).forEach{ e->
            set.add(OperateType.getByValue(e.operateType))
        }
       return set
    }
    fun getListByUserIdAndPostId(userId: Long, postId: Long): List<PostOperationEntity> {
        val query = QueryWrapper<PostOperationEntity>()
        query.eq("user_id", userId).eq("post_id", postId)
        return baseMapper.selectList(query)
    }
}