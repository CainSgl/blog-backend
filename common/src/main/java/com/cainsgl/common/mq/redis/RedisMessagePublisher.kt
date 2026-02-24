package com.cainsgl.common.mq.redis

import com.cainsgl.common.mq.MessagePublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import org.apache.rocketmq.client.core.RocketMQClientTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}
@Component
@ConditionalOnMissingBean(RocketMQClientTemplate::class)
class RedisMessagePublisher : MessagePublisher {

    @Resource
     lateinit var redisTemplate: RedisTemplate<String, Any>

    @Resource
    lateinit var listenerContainer: RedisMessageListenerContainer

    @Resource
     lateinit var consumers: List<RedisMessageConsumer>

    @PostConstruct
    fun init() {
        consumers.forEach { consumer ->
            val topic = consumer.topic()
            listenerContainer.addMessageListener({ message, _ ->
                Thread.ofVirtual().start {
                    try {
                        val body = String(message.body)
                        consumer.onMessage(body)
                    } catch (e: Exception) {
                        log.error(e) { "Redis消息消费失败: topic=$topic" }
                    }
                }
            }, ChannelTopic(topic))
            log.info { "注册Redis消费者: topic=$topic, consumer=${consumer.javaClass.simpleName}" }
        }
    }

    override fun publish(topic: String, message: Any) {
        try {
            redisTemplate.convertAndSend(topic, message)
            log.debug { "发送Redis消息: topic=$topic" }
        } catch (e: Exception) {
            log.error(e) { "发送Redis消息失败: topic=$topic" }
        }
    }
}


interface RedisMessageConsumer {
    fun topic(): String
    fun onMessage(message: String)
}
