package com.cainsgl.scheduler.job

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * 示例定时任务
 * 
 * @SchedulerLock 参数说明:
 * - name: 锁名称，全局唯一
 * - lockAtMostFor: 最大锁定时间，防止任务挂死时锁不释放
 * - lockAtLeastFor: 最小锁定时间，防止任务执行太快导致其他节点重复执行
 */
@Component
class SampleJob {

    /**
     * 示例任务: 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "sampleTask", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    fun execute() {
        logger.info { "SampleJob executed at ${LocalDateTime.now()}" }
        // TODO: 业务逻辑
    }
}
