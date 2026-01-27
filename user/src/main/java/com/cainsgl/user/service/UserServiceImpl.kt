package com.cainsgl.user.service

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.user.UserService
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.common.util.FineLockCacheUtils.getWithFineLock
import com.cainsgl.common.util.HotKeyValidator
import com.cainsgl.common.util.HotKeyValidator.Companion.HOT_KEY_COUNT_THRESHOLD
import com.cainsgl.user.repository.UserMapper
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.io.Serializable
import java.time.Duration

@Service
class UserServiceImpl : ServiceImpl<UserMapper, UserEntity>(), UserService, IService<UserEntity>
{

    @Resource
    private lateinit var redisTemplate: RedisTemplate<String, UserEntity>
    @Resource
    lateinit var hotKeyValidator: HotKeyValidator
    companion object{
        const val USER_MAX_USED_MEMORY=1024*1024*1024*3L
        const val USER_REDIS_PREFIX="user:info:"
    }

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
    
    override fun mallocMemory(userId: Long, memory: Int): Boolean {
        val updateWrapper = UpdateWrapper<UserEntity>()
        updateWrapper.eq("id", userId).setSql("used_memory = used_memory + $memory").le("used_memory",USER_MAX_USED_MEMORY-memory)
        return this.baseMapper.update(updateWrapper) > 0
    }

    override fun getById(id: Serializable?): UserEntity?
    {
        val key="${USER_REDIS_PREFIX}${id}"
        val user = redisTemplate.opsForValue().get(key)
        if(user != null)
        {
            return user
        }
        if(hotKeyValidator.isHotKey(key,count=HOT_KEY_COUNT_THRESHOLD*2))
        {
            return redisTemplate.getWithFineLock(key, { Duration.ofMinutes(20) }){
                return@getWithFineLock super<ServiceImpl>.getById(id)
            }
        }else
        {
            return super<ServiceImpl>.getById(id)
        }
    }
}
