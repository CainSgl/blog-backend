package com.cainsgl.common.entity.user

import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.VectorTypeHandler
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.TimeUnit

//为什么会专门建额外的一张用户信息表，这是为了冷热数据分离，这里的数据都是会频繁更新的，我们会缓存在redis，然后定时同步到数据库
@TableName(value = "user_extra_infos", autoResultMap = true)
data class UserExtraInfoEntity(
    @TableField("user_id")
    var userId: Long? = null,

    @TableField("follower_count")
    var followerCount: Int = 0,

    @TableField("following_count")
    var followingCount: Int = 0,

    @TableField("like_count")
    var likeCount: Int = 0,

    @TableField("comment_count")
    var commentCount: Int = 0,

    @TableField("post_count")
    var postCount: Int = 0,
    @TableField("article_view_count")
    var articleViewCount: Int = 0,

    @TableField(value = "interest_vector", typeHandler = VectorTypeHandler::class, select = false)
    var interestVector: FloatArray? = null
)
{
    companion object
    {
        const val USER_EXTRA_INFO_REDIS_PREFIX = "user:extraInfo:"
    }

    fun fillFieldByRedis(redisTemplate: RedisTemplate<String, String>): Boolean
    {
        val map = redisTemplate.opsForHash<String, String>().entries(USER_EXTRA_INFO_REDIS_PREFIX + userId)
        // 获取所有带@TableField注解的字段
        val fields = this::class.java.declaredFields.filter { it.isAnnotationPresent(TableField::class.java) }
        // 空map返回true（表示无缓存）
        if (map.isEmpty()) return false
        // 检查所有字段是否都存在，不完整返回false
        val fieldNames = fields.map { it.name }
        if (!map.keys.containsAll(fieldNames)) return false
        // 通过反射填充字段
        for (field in fields)
        {
            field.isAccessible = true
            val value = map[field.name] ?: continue
            when (field.type)
            {
                Int::class.java, Int::class.javaPrimitiveType   -> field.setInt(this, value.toInt())
                Long::class.java, Long::class.javaPrimitiveType -> field.setLong(this, value.toLong())
                FloatArray::class.java                          -> field.set(this, value.split(",").map { it.toFloat() }
                    .toFloatArray())

                else                                            -> field.set(this, value)
            }
        }
        return true
    }

    fun saveFieldByRedis(redisTemplate: RedisTemplate<String, String>): Boolean
    {
        val hashOps = redisTemplate.opsForHash<String, String>()
        val redisKey = USER_EXTRA_INFO_REDIS_PREFIX + userId
        val map = mutableMapOf<String, String>()
        // 获取所有带@TableField注解的字段，null值忽略
        val fields = this::class.java.declaredFields.filter { it.isAnnotationPresent(TableField::class.java) }
        for (field in fields)
        {
            field.isAccessible = true
            val value = field.get(this) ?: continue
            val strValue = when (value)
            {
                is FloatArray -> value.joinToString(",")
                else          -> value.toString()
            }
            map[field.name] = strValue
        }
        if (map.isNotEmpty())
        {
            hashOps.putAll(redisKey, map)
            redisTemplate.expire(redisKey, 2, TimeUnit.DAYS)
        }
        return true
    }


}
