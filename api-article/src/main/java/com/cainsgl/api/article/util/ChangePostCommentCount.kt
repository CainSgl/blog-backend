package com.cainsgl.api.article.util

import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class ChangePostCommentCount
{
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    companion object{
        const val POST_COUNT_INFO_REDIS_PREFIX = "cursor:postcount:"
    }
    fun changePostCommentCount(id: Long, count: Long): Boolean
    {
        val key="${POST_COUNT_INFO_REDIS_PREFIX}${id}"
        val opsForHash = redisTemplate.opsForHash<String,Long>()
        opsForHash.increment(key,"comment", count)
        return true
    }
}