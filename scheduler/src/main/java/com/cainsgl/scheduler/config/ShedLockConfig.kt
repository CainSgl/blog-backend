package com.cainsgl.scheduler.config

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
class ShedLockConfig {

    @Bean
    fun lockProvider(connectionFactory: RedisConnectionFactory): LockProvider {
        return RedisLockProvider(connectionFactory, "scheduler")
    }
}
val log= KotlinLogging.logger {}
@Configuration
class VirtualThreadConfig: SchedulingConfigurer
{

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val taskScheduler = ThreadPoolTaskScheduler()
        // 使用虚拟线程工厂
        try {
            val virtualThreadFactory = Thread.ofVirtual()
                .name("virtual-scheduled-")
                .factory()
            taskScheduler.setThreadFactory(virtualThreadFactory)
        } catch (e: Exception) {
            // 如果虚拟线程不可用，则使用普通线程池
            log.error{e}
            taskScheduler.threadNamePrefix = "scheduled-"
        }
        taskScheduler.initialize()
        taskRegistrar.setTaskScheduler(taskScheduler)
    }
}
