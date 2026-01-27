package com.cainsgl.scheduler.job

import com.cainsgl.api.user.extra.UserExtraInfoService
import com.cainsgl.api.user.log.UserLogService
import com.cainsgl.common.entity.user.UserExtraInfoEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.apache.rocketmq.client.core.RocketMQClientTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.Cursor
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class UserLogJob
{

    @Value("\${userLog.batchNumber}")
    var batchNumber: Int = 20

    @Resource
    lateinit var userLogService: UserLogService

    @Resource
    lateinit var rocketMQClientTemplate: RocketMQClientTemplate

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Int>

    @Resource
    lateinit var userExtraInfoService: UserExtraInfoService

//    @Scheduled(cron = "0/20 * * * * ?")
//    @SchedulerLock(name = "UserLogJob_execute", lockAtMostFor = "PT1M", lockAtLeastFor = "PT20S")
//    fun execute()
//    {
//        //发送消息，调用user的api
//        while (true)
//        {
//         //   logger.info { "定时任务触发，将同步$batchNumber 条日志" }
//            val key = userLogService.loadLogsToRedis(batchNumber)
//            if (key.isEmpty())
//            {
//                //说明无消息，直接执行成功,所有日志都被读取了
//                return
//            }
//            rocketMQClientTemplate.asyncSendNormalMessage("userLog:dispatch", key, null)
//         //   logger.info { "UserLogJob executed" }
//            Thread.sleep(100)
//        }
//    }

    @Scheduled(cron = "0/30 * * * * ?") // 每5分钟执行一次
    @SchedulerLock(name = "UserExtraInfoSyncJob_syncFromRedisToDb", lockAtMostFor = "PT2M", lockAtLeastFor = "PT30S")
    fun syncFromRedisToDb()
    {
        val cursor: Cursor<String> = redisTemplate.scan(
            ScanOptions.scanOptions().match(UserExtraInfoEntity.USER_EXTRA_INFO_REDIS_PREFIX + "*").count(1).build()
        )
        cursor.use {
            val opsForHash = redisTemplate.opsForHash<String, Int>()
            while (cursor.hasNext())
            {
                //使用分布式锁，这里需要，在往数据库写回的时候先删除数据再删除锁
                val key = cursor.next()
                opsForHash.entries(key)
                key.substringAfter(UserExtraInfoEntity.USER_EXTRA_INFO_REDIS_PREFIX).toLong()
             //   val entity = UserExtraInfoEntity(userId = id).apply { fillFieldByMap(entries) }
            }
        }
    }
}
