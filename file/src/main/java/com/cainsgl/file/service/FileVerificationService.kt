package com.cainsgl.file.service

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.cainsgl.common.entity.file.FileStatus
import com.cainsgl.common.entity.file.FileUrlEntity
import com.cainsgl.file.FileService
import com.cainsgl.file.repository.FileUrlMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * 文件验证服务
 */
@Service
class FileVerificationService(
    private val fileService: FileService,
    private val fileUrlMapper: FileUrlMapper,
    private val delayedTaskQueue: DelayedTaskQueue
) {
    companion object {
        const val DEFAULT_DELAY_SECONDS = 30 * 60L
    }
    

    fun addVerificationTask(shortUrl: Long, delaySeconds: Long = DEFAULT_DELAY_SECONDS) {
        delayedTaskQueue.addTask(shortUrl, delaySeconds)
    }
    

    @Transactional
    fun verifyFile(shortUrl: Long): Boolean {
        try {
            val fileEntity = fileUrlMapper.selectById(shortUrl) ?: return false
            
            // 已经是可用状态，跳过
            if (fileEntity.getFileStatus() == FileStatus.AVAILABLE) {
                return true
            }
            
            val extension = fileEntity.name?.substringAfterLast(".", "") ?: ""
            val sha256Hash = fileEntity.url ?: run {
                updateFileStatus(shortUrl, FileStatus.FAILED)
                return false
            }
            
            // 检查 OSS 文件是否存在
            val exists = fileService.isFileExistInOss(sha256Hash, extension)
            
            if (exists) {
                updateFileStatus(shortUrl, FileStatus.AVAILABLE)
                return true
            } else {
                updateFileStatus(shortUrl, FileStatus.FAILED)
                log.warn { "文件验证失败，OSS中不存在: shortUrl=$shortUrl" }
                return false
            }
        } catch (e: Exception) {
            log.error(e) { "文件验证异常: shortUrl=$shortUrl" }
            updateFileStatus(shortUrl, FileStatus.FAILED)
            return false
        }
    }
    

    @Transactional
    fun verifyBatch(batchSize: Int = 100): Int {
        val shortUrls = delayedTaskQueue.pollDueTasks(batchSize)
        
        if (shortUrls.isEmpty()) {
            return 0
        }
        
        var successCount = 0
        val processedShortUrls = mutableListOf<Long>()
        
        for (shortUrl in shortUrls) {
            try {
                if (verifyFile(shortUrl)) {
                    successCount++
                }
                processedShortUrls.add(shortUrl)
            } catch (e: Exception) {
                log.error(e) { "验证文件异常: shortUrl=$shortUrl" }
                processedShortUrls.add(shortUrl)
            }
        }
        
        if (processedShortUrls.isNotEmpty()) {
            delayedTaskQueue.removeTasks(processedShortUrls)
        }
        
        log.info { "批量验证完成: total=${shortUrls.size}, success=$successCount" }
        return successCount
    }

    private fun updateFileStatus(shortUrl: Long, status: FileStatus) {
        val wrapper = UpdateWrapper<FileUrlEntity>()
            .eq("short_url", shortUrl)
            .set("status", status.code)
        fileUrlMapper.update(null, wrapper)
    }
    

    fun getPendingTaskCount(): Long {
        return delayedTaskQueue.getQueueSize()
    }
}
