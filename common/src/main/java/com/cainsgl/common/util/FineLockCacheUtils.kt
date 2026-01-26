package com.cainsgl.common.util

import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class LockInt
{
    private var num: Int = 0;
    fun tryRemove(): Boolean
    {
        return --num == 0
    }

    fun lock()
    {
        num++;
    }
}


object FineLockCacheUtils
{
    // 锁对象池
    private val LOCK_OBJ_POOL = ConcurrentHashMap<String, LockInt>()
    fun <T : Any> RedisTemplate<String, T>.getWithFineLock(
        cacheKey: String, expireTime: Duration, loader: () -> T?
    ): T?
    {
        return this.getWithFineLock(cacheKey, { expireTime }, loader)
    }

    /**
     * 双重检查锁
     */
    fun <T : Any> RedisTemplate<String, T>.getWithFineLock(
        cacheKey: String,
        expireTimeGetter: (t: T?) -> Duration?,
        loader: () -> T?,
    ): T?
    {
        // 检查缓存
        val cacheData = this.opsForValue().get(cacheKey)
        if (cacheData != null)
        {
            // 重置缓存过期时间
            val expireTime = expireTimeGetter(cacheData)
            if (expireTime != null)
            {
                this.expire(cacheKey, expireTime)
            }
            return cacheData
        }
        return this.withFineLockByDoubleChecked(cacheKey,expireTimeGetter, loader)
    }

    /**
     * 该方法是直接上锁后检查，对于double checked的后半部分
     */
    fun <T : Any> RedisTemplate<String, T>.withFineLockByDoubleChecked(
        cacheKey: String,
        expireTimeGetter: (t: T?) -> Duration?,
        loader: () -> T?,
    ): T?{
        return withFineLock(cacheKey) {
            // 双重检查
            val doubleCheckData = this.opsForValue().get(cacheKey)
            val loadDataTime = expireTimeGetter(doubleCheckData)
            if (doubleCheckData != null)
            {
                if (loadDataTime != null) this.expire(cacheKey, loadDataTime)
                return@withFineLock doubleCheckData
            }
            // 缓存未命中
            val loadData: T? = loader()
            // 写入缓存
            if (loadData != null)
            {
                val loadDataTime2 = expireTimeGetter(loadData)
                if (loadDataTime2 != null) this.opsForValue().set(cacheKey, loadData, loadDataTime2)
            }
            return@withFineLock loadData
        }
    }

    /**
     * 该方法对于的synchronized部分，这里作了一层统一封装处理，保证不内存泄露。
     */
    fun <T : Any> withFineLock(cacheKey: String, operate: () -> T?): T?
    {
        val lockObj = LOCK_OBJ_POOL.putIfAbsent(cacheKey, LockInt()) ?: LOCK_OBJ_POOL[cacheKey]!!
        synchronized(lockObj) {
            lockObj.lock()
            try
            {
                return operate()
            } finally
            {
                if (lockObj.tryRemove())
                {
                    LOCK_OBJ_POOL.remove(cacheKey)
                }
            }
        }
    }
}
