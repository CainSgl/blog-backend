package com.cainsgl.user.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.user.extra.UserExtraInfoService
import com.cainsgl.common.entity.user.UserExtraInfoEntity
import com.cainsgl.common.util.FineLockCacheUtils.withFineLockByDoubleChecked
import com.cainsgl.common.util.HotKeyValidator
import com.cainsgl.common.util.HotKeyValidator.Companion.HOT_KEY_COUNT_THRESHOLD
import com.cainsgl.common.util.UserHotInfoUtils.Companion.USER_HOT_INFO_COUNT
import com.cainsgl.user.repository.UserExtraInfoMapper
import com.cainsgl.user.service.UserServiceImpl.Companion.USER_REDIS_PREFIX
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.io.Serializable
import java.time.Duration

private val logger = KotlinLogging.logger {}
@Service
class UserExtraInfoServiceImpl : ServiceImpl<UserExtraInfoMapper, UserExtraInfoEntity>(), UserExtraInfoService, IService<UserExtraInfoEntity>
{
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    /**
     * fixBug
     *  No qualifying bean of type 'RedisTemplate<String, UserExtraInfoEntity>' available at least 1 bean which qualifies as autowire candidate.
     *  这个bug很奇怪，不知道为什么这里无法注入
     */
//    @Resource
//    lateinit var redisTemplateEntity: RedisTemplate<String, UserExtraInfoEntity>

    @Resource
    lateinit var hotKeyValidator: HotKeyValidator

    companion object
    {
        const val USER_EXTRA_PREFIX_KEY = "user:extra"



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
            val key = "${USER_EXTRA_PREFIX_KEY}${id}"
            val value = redisTemplate.opsForValue().get(key) as UserExtraInfoEntity?
            if (value != null)
            {
                return value
            }
            //这里和user去公用一个key即可
            if (hotKeyValidator.isHotKey("${USER_REDIS_PREFIX}${id}", count = HOT_KEY_COUNT_THRESHOLD * 2))
            {
                return redisTemplate.withFineLockByDoubleChecked(key, { Duration.ofMinutes(20) }) {
                    return@withFineLockByDoubleChecked super<ServiceImpl>.getById(id) as Any?
                } as UserExtraInfoEntity?
            } else
            {
                return super<ServiceImpl>.getById(id)
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
        //TODO，目前将Vector不会起任何作用
        userExtraInfoEntity.interestVector = FloatArray(1024)
        userExtraInfoEntity.interestVector!!.fill(1e-8f)
        save(userExtraInfoEntity)
        return userExtraInfoEntity
    }

    fun incrFlowCount(followerId: Long, followeeId: Long): Boolean
    {
        return changeFlowCount(followerId, followeeId, 1)

    }

    fun changeFlowCount(followerId: Long, followeeId: Long, count: Long): Boolean
    {
        //followerId是粉丝，另外一个是关注的
        val followerKey = "${USER_HOT_INFO_COUNT}${followerId}"
        val opsForHash = redisTemplate.opsForHash<String, Long>()

        //增加关注数
        opsForHash.increment(followerKey, "followingCount", count)
        val followeeKey = "${USER_HOT_INFO_COUNT}${followeeId}"
        //增加粉丝数
        opsForHash.increment(followeeKey, "followerCount", count)
        return true
    }

    fun decrFlowCount(followerId: Long, followeeId: Long): Boolean
    {
        return changeFlowCount(followerId, followeeId, -1)
    }
    @Deprecated("现在微服务完全自洽")
    override fun saveCount(userExtraInfoList: List<UserExtraInfoEntity>): Boolean
    {
        throw IllegalArgumentException("废弃的api")
    }
}
