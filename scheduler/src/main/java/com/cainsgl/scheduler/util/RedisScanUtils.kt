package com.cainsgl.scheduler.util

import org.springframework.data.redis.core.Cursor
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions


class RedisScanUtils
{

    fun RedisTemplate<String,Any>.scanKeysWithPrefix(prefix: String, batchSize: Long): Set<String>
    {
        val keys: MutableSet<String> = HashSet()
        val cursor: Cursor<String> = this.scan(ScanOptions.scanOptions().match(prefix).count(batchSize).build())
        
        try {
            while (cursor.hasNext()) {
                keys.add(cursor.next())
            }
        } finally {
            cursor.close()
        }

        return keys
    }
}