package com.cainsgl.user.service

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.user.UserGroupEntity
import com.cainsgl.common.exception.BSystemException
import com.cainsgl.user.dto.response.vo.CollectType
import com.cainsgl.user.repository.UserGroupMapper
import org.springframework.stereotype.Service

@Service
class UserGroupServiceImpl : ServiceImpl<UserGroupMapper, UserGroupEntity>(), IService<UserGroupEntity>
{
    /**
     * @param needPublish 为true代表只获取公开
     */
    fun getByUserIdAndType(userId: Long, type: String?, needPublish:Boolean=false): Map<String, List<UserGroupEntity>> {
        val queryWrapper = KtQueryWrapper(UserGroupEntity::class.java)
        queryWrapper.eq(UserGroupEntity::userId, userId)
            .apply {
                if (type != null) {
                    eq(UserGroupEntity::type, CollectType.fromStr(type).code)
                }
                if (needPublish) {
                    eq(UserGroupEntity::publish, true)
                }
            }
        return baseMapper.selectList(queryWrapper).groupBy { CollectType.fromNumber(it.type ?: -1).str }
    }
    fun addGroup(userId: Long, type:String,name:String,description:String,publish: Boolean): UserGroupEntity {
        val fromStr = CollectType.fromStr(type)
        if(fromStr == CollectType.UNKNOWN) {
            throw BSystemException("未知的收藏类型")
        }
        val group = UserGroupEntity(userId=userId,type=fromStr.code,name=name, description = description,publish = publish)
        baseMapper.insert(group)
        return group
    }
}