package com.cainsgl.user.task

import com.cainsgl.common.util.user.UserHotInfoUtils.Companion.USER_HOT_INFO_COUNT
import com.cainsgl.user.service.UserExtraInfoServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import oshi.SystemInfo
import oshi.hardware.CentralProcessor


private val logger = KotlinLogging.logger {}
@Component
class UserIdleTriggeredTask
{
    private val systemInfo = SystemInfo()
    private val processor: CentralProcessor = systemInfo.hardware.processor
    private var prevTicks: LongArray = processor.systemCpuLoadTicks

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @Resource
    lateinit var userExtraInfoService: UserExtraInfoServiceImpl

    @Value("\${idle.task.cpu-threshold:40.0}")
    private val cpuThreshold = 0.0 // CPU空闲阈值：使用率低于此值时执行任务

    @Value("\${idle.task.check-interval:3000}")
    private val checkInterval: String? = null // 检查间隔

    @Value("\${idle.task.batch-size:300}")
    private val batchSize: Int = 300 // 每次扫描的key数量

    @Value("\${idle.task.db-batch-size:30}")
    private val dbBatchSize: Int = 30 // 数据库批量更新大小

    // 两阶段删除：先移动到备份key，处理成功后再删除备份
    private val getAndMoveScript = """
        local data = redis.call('HGETALL', KEYS[1])
        if #data > 0 then
            redis.call('RENAME', KEYS[1], KEYS[2])
            redis.call('EXPIRE', KEYS[2], 3600)
        end
        return data
    """.trimIndent()

    private val deleteBackupScript = """
        redis.call('DEL', KEYS[1])
        return 1
    """.trimIndent()
    @Scheduled(fixedRateString = "\${idle.task.check-interval:5000}")
    fun checkIdleAndExecute()
    {
        val cpuUsage = getCpuUsage()
        if (cpuUsage < cpuThreshold) {
            logger.debug { "User服务，低负载：doTask 将redis的数据同步到数据库里去" }
            doTask()
        }
    }

    private fun getCpuUsage(): Double {
        val currentTicks = processor.systemCpuLoadTicks
        val cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100
        prevTicks = currentTicks
        return cpuLoad
    }

    private fun doTask() {
        syncUserHotInfoToDatabase()
    }

    // 扫描Redis中的用户热点信息并同步到数据库
    private fun syncUserHotInfoToDatabase() {
        try {
            val cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match("${USER_HOT_INFO_COUNT}*").count(batchSize.toLong()).build()
            )
            
            // 创建批量更新器
            val batchUpdater = BatchUserExtraInfoUpdater(userExtraInfoService.baseMapper, dbBatchSize)
            val backupKeys = mutableListOf<String>()
            
            cursor.use {
                var processedCount = 0
                
                while (cursor.hasNext() && processedCount < batchSize) {
                    val key = cursor.next()
                    
                    try {
                        // 生成备份key
                        val backupKey = "${key}:backup"
                        
                        // 使用Lua脚本原子性地获取并移动到备份key
                        val entries = getAndMoveToBackup(key, backupKey)
                        if (entries.isEmpty()) continue
                        
                        // 提取userId
                        val userId = key.substringAfter(USER_HOT_INFO_COUNT).toLongOrNull() ?: continue
                        
                        // 使用批量更新器累积更新
                        batchUpdater.update(userId, entries)
                        
                        // 记录备份key，待数据库更新成功后删除
                        backupKeys.add(backupKey)
                        processedCount++
                    } catch (e: Exception) {
                        logger.error(e) { "处理key失败: $key" }
                    }
                }
                
                // 刷新残留数据到数据库
                batchUpdater.flush()
                
                // 数据库更新成功后，删除所有备份key
                deleteBackupKeys(backupKeys)
                
                // 只在处理了数据时才打印日志
                if (processedCount > 0) {
                    logger.info { "同步用户热点信息完成，共处理 $processedCount 个key" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "同步用户热点信息失败" }
        }
    }
    private fun getAndMoveToBackup(key: String, backupKey: String): Map<String, Long> {
        return try {
            // 使用 RedisCallback 直接操作底层连接，绕过序列化器
            val result = redisTemplate.execute<Any> { connection ->
                val keyBytes = RedisSerializer.string().serialize(key)!!
                val backupKeyBytes = RedisSerializer.string().serialize(backupKey)!!
                val scriptBytes = getAndMoveScript.toByteArray()
                
                // 执行 Lua 脚本
                connection.eval(scriptBytes, org.springframework.data.redis.connection.ReturnType.MULTI, 2, keyBytes, backupKeyBytes)
            }
            
            // 处理返回结果
            if (result == null) return emptyMap()
            
            @Suppress("UNCHECKED_CAST")
            val list = result as? List<ByteArray> ?: return emptyMap()
            
            if (list.isEmpty()) return emptyMap()
            
            // 将字节数组列表转换为 Map
            list.chunked(2).associate { chunk ->
                val k = String(chunk[0])
                val v = String(chunk[1]).toLong()
                k to v
            }
        } catch (e: Exception) {
            logger.error(e) { "执行Lua脚本失败: $key" }
            emptyMap()
        }
    }

    private fun deleteBackupKeys(backupKeys: List<String>) {
        if (backupKeys.isEmpty()) return
        
        try {
            backupKeys.forEach { backupKey ->
                try {
                    redisTemplate.execute<Any> { connection ->
                        val keyBytes = RedisSerializer.string().serialize(backupKey)!!
                        val scriptBytes = deleteBackupScript.toByteArray()
                        connection.eval(scriptBytes, org.springframework.data.redis.connection.ReturnType.INTEGER, 1, keyBytes)
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "删除备份key失败: $backupKey (将在1小时后自动过期)" }
                }
            }
            logger.debug { "成功删除 ${backupKeys.size} 个备份key" }
        } catch (e: Exception) {
            logger.error(e) { "批量删除备份key失败" }
        }
    }
}
