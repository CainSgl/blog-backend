package com.cainsgl.file.task

import com.cainsgl.file.service.FileVerificationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * 文件验证定时任务
 */
@Component
class FileVerificationTask(
    private val fileVerificationService: FileVerificationService
) {
    

    @Scheduled(cron = "0 * * * * ?")
    fun verifyFiles() {
        try {
            val pendingCount = fileVerificationService.getPendingTaskCount()
            if (pendingCount == 0L) {
                return
            }
            
            // 每次最多处理 100 个任务
            fileVerificationService.verifyBatch(100)
        } catch (e: Exception) {
            log.error(e) { "文件验证任务执行失败" }
        }
    }
}
