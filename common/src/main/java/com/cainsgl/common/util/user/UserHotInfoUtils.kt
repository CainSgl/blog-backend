package com.cainsgl.common.util.user

import org.springframework.data.redis.core.RedisTemplate

class UserHotInfoUtils
{
    companion object
    {
        const val USER_HOT_INFO_COUNT = "cursor:user:extra:"
        private fun RedisTemplate<Any, Any>.addCount(count: Long, userId: Long, filed: String)
        {
            val key = "${USER_HOT_INFO_COUNT}${userId}"
            val opsForHash = this.opsForHash<String, Long>()
            opsForHash.increment(key, filed, count)
        }

        fun RedisTemplate<Any, Any>.changeLikeCount(count: Long, userId: Long)
        {
            addCount(count, userId, "likeCount")
        }

        fun RedisTemplate<Any, Any>.changeCommentCount(count: Long, userId: Long)
        {
            addCount(count, userId, "commentCount")
        }
        fun RedisTemplate<Any, Any>.changePostCount(count: Long, userId: Long)
        {
            addCount(count, userId, "postCount")
        }
        fun RedisTemplate<Any, Any>.changeViewCount(count: Long, userId: Long)
        {
            addCount(count, userId, "articleViewCount")
        }
        fun RedisTemplate<Any, Any>.changeMsgCount(count: Long, userId: Long)
        {
            addCount(count, userId, "msgCount")
        }
    }


}