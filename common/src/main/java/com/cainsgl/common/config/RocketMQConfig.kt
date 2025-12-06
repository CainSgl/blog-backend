package com.cainsgl.common.config

import org.apache.rocketmq.client.core.RocketMQClientTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Configuration
@ConditionalOnClass(RocketMQClientTemplate::class)
class RocketMQConfig
{
    @Bean
    fun rocketMQConsumerExecutor(): Executor {
        return Executors.newVirtualThreadPerTaskExecutor()
    }
}
