package com.cainsgl.common.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.stereotype.Component

/**
 * 热点Key检测器
 * 通过访问计数判断key是否为热点key
 */
private val logger = KotlinLogging.logger {}
@Component
@ConditionalOnClass(RedisConnectionFactory::class)
class HotKeyValidator(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    
    companion object {
        // 阈值
        const val HOT_KEY_COUNT_THRESHOLD = 8L
        // 时间窗口
         const val TIME_WINDOW_SECONDS = 2L
        private val LUA_SCRIPT = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            if count >= tonumber(ARGV[2]) then
                return 1
            else
                return 0
            end
        """.trimIndent()
    }
    
    /**
     * 判断key是否为热点key
     * 每次调用会自增访问计数，如果在时间窗口内访问次数超过阈值则返回true
     */
    fun isHotKey(key: String,count:Long=HOT_KEY_COUNT_THRESHOLD,time:Long=TIME_WINDOW_SECONDS): Boolean {
        return try {
            val hotKeyCounterKey = "hotkey:$key"
            val result = redisTemplate.execute<Long> { connection ->
                val keyBytes = RedisSerializer.string().serialize(hotKeyCounterKey)!!
                val scriptBytes = LUA_SCRIPT.toByteArray()
                val timeBytes = time.toString().toByteArray()
                val countBytes = count.toString().toByteArray()
                
                connection.eval(scriptBytes, ReturnType.INTEGER, 1, keyBytes, timeBytes, countBytes)
            }
            result == 1L
        } catch (e: Exception) {
            logger.error(e) { "Error checking redis" }
            false
        }
    }
}