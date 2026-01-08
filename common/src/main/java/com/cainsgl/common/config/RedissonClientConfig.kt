package com.cainsgl.common.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
@ConditionalOnClass(org.redisson.api.RedissonClient::class)
class RedissonClientConfig
{
    @Bean
    fun redissonClient(redisProperties: RedisProperties): RedissonClient
    {
        val config = Config()
        redisProperties.timeout.toMillis().toInt()
        val maxActive = redisProperties.jedis?.pool?.maxActive ?: 10
        val maxWait = redisProperties.jedis?.pool?.maxWait?.toMillis()?.toInt() ?: 1000
        config.useSingleServer()
            .setAddress("redis://${redisProperties.host}:${redisProperties.port}")
            .setPassword(redisProperties.password ?: "")
            .setDatabase(redisProperties.database)
            .setConnectionPoolSize((maxActive / 2).coerceAtLeast(2))
            .setConnectionMinimumIdleSize(2)
            .setIdleConnectionTimeout(redisProperties.connectTimeout?.toMillis()?.toInt() ?: 5000)
            .setConnectTimeout(redisProperties.connectTimeout?.toMillis()?.toInt()?:5000)
            .setTimeout(redisProperties.timeout?.toMillis()?.toInt() ?: 5000)
            .setRetryAttempts(3)
            .setRetryInterval((maxWait / 10).coerceAtLeast(100))
        config.setNettyThreads( (Runtime.getRuntime().availableProcessors() / 4).coerceAtLeast(1) );
        return Redisson.create(config)
    }
}