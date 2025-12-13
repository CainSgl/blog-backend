package com.cainsgl.user.service

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.user.UserService
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.user.repository.UserMapper
import org.springframework.stereotype.Service

@Service
class UserServiceImpl : ServiceImpl<UserMapper, UserEntity>(), UserService, IService<UserEntity>
{
    /**
     * 根据账号和密码和邮件获取用户信息
     * @param account
     * @return
     */
    fun getUserByAccount(account: String): UserEntity?
    {
        val queryWrapper: QueryWrapper<UserEntity>  = QueryWrapper<UserEntity>()
        queryWrapper.eq("username", account).or().eq("email", account).or().eq("phone", account)
        return this.getOne(queryWrapper)
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
