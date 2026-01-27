package com.cainsgl.article.task

import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.article.service.PostServiceImpl.Companion.POST_COUNT_INFO_REDIS_PREFIX
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
class PostIdleTriggeredTask {
    private val systemInfo = SystemInfo()
    private val processor: CentralProcessor = systemInfo.hardware.processor
    private var prevTicks: LongArray = processor.systemCpuLoadTicks

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @Resource
    lateinit var postService: PostServiceImpl

    @Value("\${idle.task.cpu-threshold:40.0}")
    private val cpuThreshold = 0.0 // CPU空闲阈值：使用率低于此值时执行任务

    @Value("\${idle.task.check-interval:3000}")
    private val checkInterval: String? = null // 检查间隔

    @Value("\${idle.task.batch-size:300}")
    private val batchSize: Int = 300 // 每次扫描的key数量

    @Value("\${idle.task.db-batch-size:30}")
    private val dbBatchSize: Int = 30 // 数据库批量更新大小

    private val getAndDeleteScript = """
        local data = redis.call('HGETALL', KEYS[1])
        if #data > 0 then
            redis.call('DEL', KEYS[1])
        end
        return data
    """.trimIndent()

    @Scheduled(fixedRateString = "\${idle.task.check-interval:5000}")
    fun checkIdleAndExecute() {
        val cpuUsage = getCpuUsage()
        if (cpuUsage < cpuThreshold) {
            logger.debug { "Article服务，低负载：doTask 将redis的数据同步到数据库里去" }
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
        syncPostCountToDatabase()
    }

    // 扫描Redis中的文章计数信息并同步到数据库
    private fun syncPostCountToDatabase() {
        try {
            val cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match("${POST_COUNT_INFO_REDIS_PREFIX}*").count(batchSize.toLong()).build()
            )

            // 创建批量更新器
            val batchUpdater = BatchPostCountUpdater(postService.baseMapper, dbBatchSize)

            cursor.use {
                var processedCount = 0

                while (cursor.hasNext() && processedCount < batchSize) {
                    val key = cursor.next()

                    try {
                        // 使用Lua脚本原子性地获取并删除key
                        val entries = getAndDeleteHash(key)
                        if (entries.isEmpty()) continue

                        // 解析key: cursor:postcount:{postId}
                        val postId = key.substringAfter(POST_COUNT_INFO_REDIS_PREFIX).toLongOrNull() ?: continue

                        // 遍历hash中的所有字段（type）并使用批量更新器累积更新
                        entries.forEach { (type, value) ->
                            if (value > 0) {
                                batchUpdater.update(postId, type, value)
                            }
                        }
                        processedCount++
                    } catch (e: Exception) {
                        logger.error(e) { "处理key失败: $key" }
                    }
                }

                // 刷新残留数据
                batchUpdater.flush()

                // 只在处理了数据时才打印日志
                if (processedCount > 0) {
                    logger.info { "同步文章计数信息完成，共处理 $processedCount 个key" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "同步文章计数信息失败" }
        }
    }

    private fun getAndDeleteHash(key: String): Map<String, Long> {
        return try {
            // 使用 RedisCallback 直接操作底层连接，绕过序列化器
            val result = redisTemplate.execute<Any> { connection ->
                val keyBytes = RedisSerializer.string().serialize(key)!!
                val scriptBytes = getAndDeleteScript.toByteArray()
                connection.eval(scriptBytes, org.springframework.data.redis.connection.ReturnType.MULTI, 1, keyBytes)
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
}