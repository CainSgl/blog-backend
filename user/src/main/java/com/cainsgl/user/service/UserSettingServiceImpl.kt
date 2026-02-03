package com.cainsgl.user.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.user.UserSettingEntity
import com.cainsgl.user.repository.UserSettingMapper
import org.springframework.stereotype.Service

@Service
class UserSettingServiceImpl : ServiceImpl<UserSettingMapper, UserSettingEntity>(), IService<UserSettingEntity>
{
    /**
     * 获取用户设置
     */
    fun getUserSetting(userId: Long): UserSettingEntity?
    {
        return this.getById(userId)
    }

    /**
     * 保存或更新用户设置
     */
    fun saveOrUpdateSetting(userId: Long, json: Map<String, Any>): Boolean
    {
        val entity = UserSettingEntity(userId = userId, json = json)
        return this.saveOrUpdate(entity)
    }

    /**
     * 删除用户设置
     */
    fun deleteSetting(userId: Long): Boolean
    {
        return this.removeById(userId)
    }
}
