package com.cainsgl.user.service

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.user.log.UserLogService
import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.common.exception.BSystemException
import com.cainsgl.user.repository.UserLogMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate


private val logger = KotlinLogging.logger {}

@Service
class UserLogServiceImpl : ServiceImpl<UserLogMapper, UserLogEntity>(), UserLogService, IService<UserLogEntity>
{
//    @Resource
//    lateinit var logDispatcher: LogDispatcher
    companion object{
        const val REDIS_PREFIX_LOGS="user:logs"
    }

    @Resource
    private lateinit var redisTemplate: RedisTemplate<Any, Any>



    @Resource
    lateinit var transactionTemplate: TransactionTemplate

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun loadLogsToRedis(value: Int): String
    {
        //直接从userLog里获取，统计完成后再往旧数据里放
        val queryWrapper = KtQueryWrapper(UserLogEntity::class.java)
        queryWrapper.last("limit $value").eq(UserLogEntity::processed, false)
        var list: List<UserLogEntity>? = null
        val size = transactionTemplate.execute { status ->
            list = list(queryWrapper)
            if(list.isNullOrEmpty())
            {
                return@execute 0
            }
            val ids = list.mapNotNull { it.id }
            val update= KtUpdateWrapper(UserLogEntity::class.java).set(UserLogEntity::processed,true).`in`(UserLogEntity::id,ids)
            update(update)
            return@execute list.size
        }
        if (size == null || size < -1)
        {
            logger.error { "无法从数据库加载用户日志到redis，似乎是数据库出错" }
            throw BSystemException("无法获取用户日志，似乎是数据库出错")
        }
        if(list!!.isEmpty())
        {
            return ""
        }
        val key= REDIS_PREFIX_LOGS
        //数据丢失了也无所谓，只是一些不重要的日志，容忍
        redisTemplate.opsForList().rightPushAll(key, list!!)
        return key
    }
}
