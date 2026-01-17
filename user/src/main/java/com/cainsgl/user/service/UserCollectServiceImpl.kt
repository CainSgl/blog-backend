package com.cainsgl.user.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.user.UserCollectEntity
import com.cainsgl.user.repository.UserCollectMapper
import org.springframework.stereotype.Service

@Service
class UserCollectServiceImpl : ServiceImpl<UserCollectMapper, UserCollectEntity>(), IService<UserCollectEntity>
{
    fun listByUserId(groupId: Long,userId: Long): List<UserCollectEntity>
    {
        val query = QueryWrapper<UserCollectEntity>()
        query.eq("group_id", groupId)
        return baseMapper.selectList(query)
    }
}