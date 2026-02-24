package com.cainsgl.common.mq.rocketmq

import com.cainsgl.common.mq.MessagePublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.apache.rocketmq.client.core.RocketMQClientTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnBean(RocketMQClientTemplate::class)
class RocketMQMessagePublisher : MessagePublisher {

    @Resource
    lateinit var rocketMQClientTemplate: RocketMQClientTemplate

    override fun publish(topic: String, message: Any) {
        try {
            rocketMQClientTemplate.asyncSendNormalMessage(topic, message, null)
            log.debug { "发送RocketMQ消息: topic=$topic" }
        } catch (e: Exception) {
            log.error(e) { "发送RocketMQ消息失败: topic=$topic" }
        }
    }
}
