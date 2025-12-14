package com.cainsgl.consumer.user

import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.consumer.core.BaseConsumer
import com.cainsgl.consumer.user.log.LogDispatcher
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.apache.rocketmq.client.annotation.RocketMQMessageListener
import org.apache.rocketmq.client.apis.consumer.ConsumeResult
import org.apache.rocketmq.client.apis.message.MessageView
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 文章发布消费者
 */
private val log = KotlinLogging.logger { }

@Component
@RocketMQMessageListener(
    consumerGroup = "userLog-consumer",
    topic = "userLog",
    tag = "dispatch",
    consumptionThreadCount = 15
)
class UserLogConsumer : BaseConsumer<String>(String::class.java)
{
    override val enableIdempotent: Boolean = true
    @Value("\${userLog.batchNumber}")
    var batchNumber:Int=20
    @Resource
    lateinit var logDispatcher: LogDispatcher

    override fun doConsume(message: String, messageView: MessageView): ConsumeResult
    {
        val allLogs = mutableListOf<UserLogEntity>()
        //从redis读取日志，通过message里的内容，message里的就是key
        while (true)
        {
            val entity = redisTemplate.opsForList().leftPop(message) as? UserLogEntity ?: break
            allLogs.add(entity)
            //一次处理这么多的日志
            if(allLogs.size > batchNumber*2)
            {
                log.warn{"似乎出现了日志堆积，本次处理处理数量:${allLogs.size}"}
                break
            }
        }
        //处理成功后会删除他们的
        val remainingLogs = logDispatcher.batchDispatch(allLogs)
        if (remainingLogs.isNotEmpty())
        {
            //剩余的日志，直接抛弃了，应该是无法处理的
            //TODO 处理后的残留日志处理，这里需要
            log.warn { "Remaining logs: $remainingLogs" }
        }
        return ConsumeResult.SUCCESS
    }
}