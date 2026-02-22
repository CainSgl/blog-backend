package com.cainsgl.file.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * 文件验证服务测试
 * 
 * 注意：这是一个集成测试示例，需要 Redis 和数据库环境
 */
@SpringBootTest
class FileVerificationServiceTest {
    
    @Autowired
    private lateinit var fileVerificationService: FileVerificationService
    
    @Autowired
    private lateinit var delayedTaskQueue: DelayedTaskQueue
    
    /**
     * 测试添加验证任务
     */
    @Test
    fun testAddVerificationTask() {
        fileVerificationService.addVerificationTask(
            shortUrl = 999999L,
            delaySeconds = 60L  // 1分钟后验证
        )
        
        println("任务添加成功，队列大小: ${fileVerificationService.getPendingTaskCount()}")
    }
    
    /**
     * 测试立即验证（用于调试）
     */
    @Test
    fun testImmediateVerification() {
        // 添加一个立即到期的任务
        fileVerificationService.addVerificationTask(
            shortUrl = 999999L,
            delaySeconds = 0L  // 立即验证
        )
        
        // 等待1秒
        Thread.sleep(1000)
        
        // 执行验证
        val successCount = fileVerificationService.verifyBatch(10)
        println("验证完成，成功数: $successCount")
    }
    
    /**
     * 测试获取到期任务
     */
    @Test
    fun testPollDueTasks() {
        val shortUrls = delayedTaskQueue.pollDueTasks(10)
        println("获取到期任务数: ${shortUrls.size}")
        
        shortUrls.forEach { shortUrl ->
            println("任务: shortUrl=$shortUrl")
        }
    }
    
    /**
     * 测试队列状态
     */
    @Test
    fun testQueueStatus() {
        val queueSize = fileVerificationService.getPendingTaskCount()
        println("当前队列大小: $queueSize")
    }
}
