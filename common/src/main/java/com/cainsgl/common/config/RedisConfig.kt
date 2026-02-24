package com.cainsgl.common.config

import com.alibaba.fastjson2.support.spring6.data.redis.GenericFastJsonRedisSerializer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
import java.util.concurrent.Executors

val log= KotlinLogging.logger {}
@Configuration
@ConditionalOnClass(RedisConnectionFactory::class)
class RedisConfig
{
    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any>
    {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        // key使用String序列化
        val stringSerializer = StringRedisSerializer()
        template.keySerializer = stringSerializer
        template.hashKeySerializer = stringSerializer
        // value使用FastJSON2序列化
        val fastJsonSerializer = GenericFastJsonRedisSerializer()
        template.valueSerializer = fastJsonSerializer
        template.hashValueSerializer = fastJsonSerializer
        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun redisMessageListenerContainer(connectionFactory: RedisConnectionFactory): RedisMessageListenerContainer
    {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        return container
    }

    @Bean
    fun taskExecutor(): TaskExecutor?
    {
        return try {
            val virtualThreadFactory = Thread.ofVirtual()
                .name("virtual-task-")
                .factory()
            ConcurrentTaskExecutor(Executors.newThreadPerTaskExecutor(virtualThreadFactory))
        } catch (e: Exception) {
            log.error { "Failed to create virtual thread executor: ${e.message}" }
            return null
        }
    }
}
