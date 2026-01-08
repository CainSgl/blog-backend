package com.cainsgl.user.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.user.extra.UserExtraInfoService
import com.cainsgl.common.entity.user.UserExtraInfoEntity
import com.cainsgl.user.misc.IncrSessionCallback
import com.cainsgl.user.repository.UserExtraInfoMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.redisson.api.RedissonClient
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.io.Serializable

private val logger = KotlinLogging.logger {}

@Service
class UserExtraInfoServiceImpl : ServiceImpl<UserExtraInfoMapper, UserExtraInfoEntity>(), UserExtraInfoService,
    IService<UserExtraInfoEntity>
{
    @Resource
    lateinit var redissonClient: RedissonClient

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Int>

    companion object
    {
        const val USER_EXTRA_PREFIX_KEY="user:extra"
    }

    override fun getInterestVector(userId: Long): FloatArray?
    {
        val queryWrapper = QueryWrapper<UserExtraInfoEntity>()
        queryWrapper.select("interest_vector")
        queryWrapper.eq("user_id", userId)
        val selectOne = baseMapper.selectOne(queryWrapper)
        return selectOne.interestVector
    }

    override fun setInterestVector(userId: Long, values: FloatArray): Boolean
    {
        return baseMapper.updateInterestVector(userId, values) > 0
    }

    //该方法使用双重检查锁，无需重新写save方法，后续会定时从redis里重新写会数据库
    override fun getById(id: Serializable?): UserExtraInfoEntity?
    {
        if (id == null)
        {
            return null
        }
        if (id is Long)
        {
            //优先从redis里读取
            val ueInfo = UserExtraInfoEntity(userId = id)
            if (ueInfo.fillFieldByRedis(redisTemplate))
            {
                return ueInfo
            }
            //说明填充失败，尝试从数据库里获取
            val lock = redissonClient.getLock("lock:"+USER_EXTRA_PREFIX_KEY + id)
            val isLockAcquired = lock.tryLock(15, java.util.concurrent.TimeUnit.SECONDS)
            if (!isLockAcquired)
            {
                //时间太长了，采用备用方案
                //   throw BSystemException("获取热点数据失败，缓存问题")
                return super<ServiceImpl>.getById(id)
            }
            try
            {
                if (ueInfo.fillFieldByRedis(redisTemplate))
                {
                    return ueInfo
                }
                val ueInfo2 = super<ServiceImpl>.getById(id) ?: return null
                ueInfo2.saveFieldByRedis(redisTemplate)
                return ueInfo2
            } catch (ex: Exception)
            {
                logger.error { "获取用户信息失败$ex" }
                return null
            } finally
            {
                lock.unlock()
            }
        }
        return null
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    fun getBySaveOnNull(id: Long): UserExtraInfoEntity
    {
        val byId = getById(id)
        if (byId != null)
        {
            return byId
        }
        val userExtraInfoEntity = UserExtraInfoEntity(userId = id)
        userExtraInfoEntity.interestVector = FloatArray(1024)
        userExtraInfoEntity.interestVector!!.fill(1e-8f)
        save(userExtraInfoEntity)
        userExtraInfoEntity.saveFieldByRedis(redisTemplate)
        return userExtraInfoEntity
    }

    fun incrFlowCount(followerId: Long, followeeId: Long)
    {
        redisTemplate.execute(
            IncrSessionCallback(
                "${UserExtraInfoEntity.USER_EXTRA_INFO_REDIS_PREFIX}$followerId",
                "${UserExtraInfoEntity.USER_EXTRA_INFO_REDIS_PREFIX}$followeeId",
                1
            )
        )
    }

    fun decrFlowCount(followerId: Long, followeeId: Long)
    {
        redisTemplate.execute(
            IncrSessionCallback(
                "${UserExtraInfoEntity.USER_EXTRA_INFO_REDIS_PREFIX}$followerId",
                "${UserExtraInfoEntity.USER_EXTRA_INFO_REDIS_PREFIX}$followeeId",
                -1
            )
        )
    }
}
