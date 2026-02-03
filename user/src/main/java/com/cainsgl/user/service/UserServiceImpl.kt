package com.cainsgl.user.service

import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.toolkit.IdWorker
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.user.UserService
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.common.entity.user.UserNoticeEntity
import com.cainsgl.common.util.FineLockCacheUtils.getWithFineLock
import com.cainsgl.common.util.HotKeyValidator
import com.cainsgl.common.util.HotKeyValidator.Companion.HOT_KEY_COUNT_THRESHOLD
import com.cainsgl.common.util.user.UserHotInfoUtils.Companion.changeMsgCount
import com.cainsgl.user.document.UserDocument
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
    lateinit var redisTemplate: RedisTemplate<String, UserEntity>

    @Resource
    lateinit var userNoticeServiceImpl: UserNoticeServiceImpl

    @Resource
    lateinit var hotKeyValidator: HotKeyValidator

    @Resource
    lateinit var userDocumentService: UserDocumentService

    companion object
    {
        const val USER_MAX_USED_MEMORY = 1024 * 1024 * 1024 * 10L
        const val USER_REDIS_PREFIX = "user:info:"
    }

    /**
     * 根据账号和密码和邮件获取用户信息
     * @param account
     * @return
     */
    fun getUserByAccount(account: String): UserEntity?
    {
        val queryWrapper = KtQueryWrapper(UserEntity::class.java)
        queryWrapper.eq(UserEntity::username, account).or().eq(UserEntity::email, account).or()
            .eq(UserEntity::phone, account)
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

    override fun mallocMemory(userId: Long, memory: Int): Boolean
    {
        val updateWrapper = KtUpdateWrapper(UserEntity::class.java)
        updateWrapper.eq(UserEntity::id, userId).setSql("used_memory = used_memory + $memory")
            .le(UserEntity::usedMemory, USER_MAX_USED_MEMORY - memory)
        return this.baseMapper.update(updateWrapper) > 0
    }

    override fun createNotice(targetId: Long, type: Int, userId: Long, targetUser: Long): Boolean
    {
        val entity =
            UserNoticeEntity(targetId = targetId, type = type.toShort(), userId = userId, targetUser = targetUser)
        userNoticeServiceImpl.save(entity)
        val redis = redisTemplate as RedisTemplate<Any, Any>
        redis.changeMsgCount(1, userId, type)
        return true
    }

    override fun getById(id: Serializable?): UserEntity?
    {
        if (id == null)
        {
            return null
        }
        val key = "${USER_REDIS_PREFIX}${id}"
        val user = redisTemplate.opsForValue().get(key)
        if (user != null)
        {
            return user
        }
        if (hotKeyValidator.isHotKey(key, count = HOT_KEY_COUNT_THRESHOLD * 2))
        {
            return redisTemplate.getWithFineLock(key, { Duration.ofSeconds(60) }) {
                return@getWithFineLock super<ServiceImpl>.getById(id)
            }
        } else
        {
            return super<ServiceImpl>.getById(id)
        }
    }

    fun crateUserBaseInfo(userEntity: UserEntity? = null): UserEntity
    {
        val genId = IdWorker.getId()
        val entity = userEntity ?: UserEntity()
        entity.apply {
            id = genId
            roles = UserEntity.DEFAULT_ROLE
            permissions = UserEntity.DEFAULT_PERMISSIONS
        }
        if (entity.gender != "男" && entity.gender != "女")
        {
            entity.gender = ""
        }
        save(entity)
        
        // 同步到 ES
        if (entity.nickname != null)
        {
            try
            {
                userDocumentService.save(UserDocument(id = entity.id!!, nickname = entity.nickname!!))
            } catch (e: Exception)
            {
                // ES 同步失败不影响主流程
            }
        }
        
        return entity
    }

    override fun updateById(entity: UserEntity): Boolean
    {
        val result = super<ServiceImpl>.updateById(entity)
        
        // 如果更新成功且包含昵称，同步到 ES
        if (result && entity.id != null && entity.nickname != null)
        {
            try
            {
                userDocumentService.save(UserDocument(id = entity.id!!, nickname = entity.nickname!!))
            } catch (e: Exception)
            {
                // ES 同步失败不影响主流程
            }
        }
        
        return result
    }
}
