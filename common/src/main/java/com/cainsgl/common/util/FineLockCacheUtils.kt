package com.cainsgl.common.util

import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger


object FineLockCacheUtils
{
    // 锁对象池
    private val LOCK_OBJ_POOL = ConcurrentHashMap<String, Any>()

    // 引用计数池
    private val REF_COUNT_POOL = ConcurrentHashMap<String, AtomicInteger>()
    fun <T : Any> RedisTemplate<String, T>.getWithFineLock(
        cacheKey: String,
        expireTime: Duration,
        loader: () -> T?,
        needLoad: (t: T?) -> Duration? = { expireTime }
    ): T?
    {
        // 检查缓存
        val cacheData = this.opsForValue().get(cacheKey)
        if (cacheData != null)
        {
            // 重置缓存过期时间
            this.expire(cacheKey, expireTime)
            return cacheData
        }
        val lockObj = LOCK_OBJ_POOL.putIfAbsent(cacheKey, Any()) ?: LOCK_OBJ_POOL[cacheKey]!!
        // 双重检查缓存
        synchronized(lockObj) {
            val refCount = REF_COUNT_POOL.putIfAbsent(cacheKey, AtomicInteger(0)) ?: REF_COUNT_POOL[cacheKey]!!
            try
            {
                refCount.incrementAndGet() // 计数+1
                // 双重检查
                val doubleCheckData = this.opsForValue().get(cacheKey)
                val loadDataTime = needLoad(doubleCheckData)
                if (doubleCheckData != null)
                {
                    if(loadDataTime!=null)
                        this.expire(cacheKey, loadDataTime)
                    return doubleCheckData
                }
                // 缓存未命中
                val loadData: T? = loader()
                // 写入缓存
                if (loadData != null)
                {
                    val loadDataTime2 = needLoad(loadData)
                    if (loadDataTime2 != null)
                        this.opsForValue().set(cacheKey, loadData, loadDataTime2)
                }
                return loadData
            } finally
            {
                // 保证计数必递减，避免内存泄漏
                val currentCount = refCount.decrementAndGet()
                // 计数归0时，删除锁对象和计数器
                if (currentCount <= 0)
                {
                    LOCK_OBJ_POOL.remove(cacheKey)
                    REF_COUNT_POOL.remove(cacheKey)
                }
            }
        }
    }


}
