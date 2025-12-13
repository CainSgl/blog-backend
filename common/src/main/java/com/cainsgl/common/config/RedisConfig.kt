package com.cainsgl.common.config

import com.alibaba.fastjson2.support.spring6.data.redis.GenericFastJsonRedisSerializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

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
}
