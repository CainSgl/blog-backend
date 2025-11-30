package com.cainsgl.user.service

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.common.service.user.UserService
import com.cainsgl.common.util.UserUtils
import com.cainsgl.user.repository.UserMapper
import org.springframework.stereotype.Service

@Service
class UserServiceImpl : ServiceImpl<UserMapper, UserEntity>(), UserService, IService<UserEntity>
{
    /**
     * 获取用户信息
     * @param id
     * @return
     */
    fun getUser(id: Long): UserEntity?
    {
        return baseMapper.selectById(id)
    }
    /**
     * 更新用户信息
     * @param userEntity
     * @return
     */
    override fun updateById(userEntity: UserEntity): Boolean
    {
        val b = super<ServiceImpl>.updateById(userEntity)
        UserUtils.setUserInfo(userEntity)
        return b
    }
    /**
     * 根据账号和密码和邮件获取用户信息
     * @param account
     * @param hashPassword
     * @return
     */
    fun getUserByAccount(account: String, hashPassword: String): UserEntity?
    {
        //账号或用户名或邮件登录，密码唯一
        val queryWrapper = LambdaQueryWrapper<UserEntity>()
            .eq(UserEntity::username, account).eq(UserEntity::passwordHash, hashPassword)
            .or()
            .eq(UserEntity::email, account).eq(UserEntity::passwordHash, hashPassword)
            .or()
            .eq(UserEntity::phone, account).eq(UserEntity::passwordHash, hashPassword)
        return baseMapper.selectOne(queryWrapper)
    }
    /**
     * 获取用户扩展信息
     * @param id
     * @return
     */
    fun getExtra(id: Long): UserEntity.Extra?
    {
        val s = baseMapper.selectExtraById(id)
        return JSON.parseObject(s, UserEntity.Extra::class.java)
    }
    /**
     * 设置用户扩展信息
     * @param id
     * @param extra
     * @return
     */
    fun setExtra(id: Long, extra: UserEntity.Extra): Boolean
    {
        val extraString = JSON.toJSONString(extra)
        val updateWrapper = UpdateWrapper<UserEntity>()
        updateWrapper.eq("id", id).set("extra", extraString)
        this.baseMapper.update(updateWrapper)
        return this.baseMapper.update(updateWrapper) > 0
    }
}
