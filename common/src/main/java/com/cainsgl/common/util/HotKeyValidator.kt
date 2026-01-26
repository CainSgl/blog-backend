package com.cainsgl.common.util

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component

/**
 * 热点Key检测器
 * 通过访问计数判断key是否为热点key
 */
@Component
@ConditionalOnClass(RedisConnectionFactory::class)
class HotKeyValidator(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    
    companion object {
        // 阈值
        const val HOT_KEY_COUNT_THRESHOLD = 6L
        // 时间窗口
         const val TIME_WINDOW_SECONDS = 30L
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
    
    private val redisScript: RedisScript<Long> = RedisScript.of(LUA_SCRIPT, Long::class.java)
    
    /**
     * 判断key是否为热点key
     * 每次调用会自增访问计数，如果在时间窗口内访问次数超过阈值则返回true
     */
    fun isHotKey(key: String,count:Long=HOT_KEY_COUNT_THRESHOLD,time:Long=TIME_WINDOW_SECONDS): Boolean {
        return try {
            val hotKeyCounterKey = "hotkey:$key"
            val result = redisTemplate.execute(
                redisScript,
                listOf(hotKeyCounterKey),
                time,
                count
            )
            result == 1L
        } catch (e: Exception) {
            false
        }
    }
}