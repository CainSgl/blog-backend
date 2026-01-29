package com.cainsgl.common.util.user

import com.cainsgl.common.entity.user.UserNoticeType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate

private val logger = KotlinLogging.logger {}
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
        fun RedisTemplate<Any, Any>.changeMsgCount(count: Long, userId: Long,type:Int)
        {
            addCount(count, userId, "msgCount")
            //根据type不同，也去增加不同的count
            val type = UserNoticeType.getByValue(type)
            if(!type.check)
            {
                //说明是哪几个unknow的
                logger.error { "unknown unknown count $count" }
                return
            }
            addCount(count, userId,type.dbField )
        }
    }


}