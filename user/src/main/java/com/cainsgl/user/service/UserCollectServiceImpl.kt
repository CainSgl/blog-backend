package com.cainsgl.user.service

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.user.UserCollectEntity
import com.cainsgl.common.entity.user.UserGroupEntity
import com.cainsgl.user.dto.response.vo.CollectType
import com.cainsgl.user.repository.UserCollectMapper
import com.cainsgl.user.repository.UserGroupMapper
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserCollectServiceImpl : ServiceImpl<UserCollectMapper, UserCollectEntity>(), IService<UserCollectEntity>
{
    @Resource
    lateinit var userGroupMapper: UserGroupMapper

    fun listByUserId(groupId: Long,userId: Long): List<UserCollectEntity>
    {
        val query = KtQueryWrapper(UserCollectEntity::class.java)
        query.eq(UserCollectEntity::groupId, groupId)
        return baseMapper.selectList(query)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun deleteByTargetIdAndType(userId:Long,targetId: Long, type: String)
    {
        val typeCode = CollectType.fromStr(type).code
        
        //查询该用户对该target的所有收藏记录
        val query = KtQueryWrapper(UserCollectEntity::class.java)
        query.eq(UserCollectEntity::userId, userId)
        query.eq(UserCollectEntity::targetId, targetId)
        val collects = baseMapper.selectList(query)
        
        if (collects.isEmpty()) {
            return
        }
        val groupIds = collects.mapNotNull { it.groupId }.toSet()
        if (groupIds.isEmpty()) {
            return
        }

        // 找出这些 group 中 type 匹配的
        val groupQuery = KtQueryWrapper(UserGroupEntity::class.java)
        groupQuery.`in`(UserGroupEntity::id, groupIds)
        groupQuery.eq(UserGroupEntity::type, typeCode)
        val validGroups = userGroupMapper.selectList(groupQuery)
        val validGroupIds = validGroups.mapNotNull { it.id }.toSet()

        //删除匹配的收藏记录并更新 count
        for (collect in collects) {
            if (collect.groupId in validGroupIds) {
                baseMapper.deleteById(collect.id)
                
                val updateWrapper = KtUpdateWrapper(UserGroupEntity::class.java)
                updateWrapper.eq(UserGroupEntity::id, collect.groupId)
                updateWrapper.setSql("count = count - 1")
                userGroupMapper.update(null, updateWrapper)
            }
        }
    }
}
