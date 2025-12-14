package com.cainsgl.scheduler.job

import com.cainsgl.api.user.log.UserLogService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.apache.rocketmq.client.core.RocketMQClientTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}
@Component
class UserLogJob {

    @Value("\${userLog.batchNumber}")
    var batchNumber:Int=20

    @Resource
    lateinit var userLogService: UserLogService
    @Resource
    lateinit var rocketMQClientTemplate: RocketMQClientTemplate
    @Scheduled(cron = "0/20 * * * * ?")
    @SchedulerLock(name = "UserLogJob_execute", lockAtMostFor = "PT1M", lockAtLeastFor = "PT20S")
    fun execute() {
        //发送消息，调用user的api
        while(true)
        {
            logger.info { "定时任务触发，将同步$batchNumber 条日志" }
            val key = userLogService.loadLogsToRedis(batchNumber)
            if(key.isEmpty()){
                //说明无消息，直接执行成功,所有日志都被读取了
                return
            }
            rocketMQClientTemplate.asyncSendNormalMessage("userLog:dispatch", key, null)
            logger.info { "UserLogJob executed" }
            Thread.sleep(100)
        }
    }
}
