package com.cainsgl.common.entity.user

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.cainsgl.common.handler.VectorTypeHandler
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

//为什么会专门建额外的一张用户信息表，这是为了冷热数据分离，这里的数据都是会频繁更新的，我们会缓存在redis，然后定时同步到数据库
@TableName(value = "user_extra_infos", autoResultMap = true)
open class UserExtraInfoEntity(
    @TableId(value = "user_id", type = IdType.INPUT)
    @field:JsonSerialize(using = ToStringSerializer::class)
    var userId: Long? = null,

    @TableField("follower_count")
    var followerCount: Int? = null,

    @TableField("following_count")
    var followingCount: Int? = null,

    @TableField("like_count")
    var likeCount: Int? = null,

    @TableField("comment_count")
    var commentCount: Int? = null,

    @TableField("post_count")
    var postCount: Int? = null,
    @TableField("article_view_count")
    var articleViewCount: Int? = null,

    @TableField(value = "interest_vector", typeHandler = VectorTypeHandler::class, select = false)
    var interestVector: FloatArray? = null
)
{
    companion object
    {
        const val USER_EXTRA_INFO_REDIS_PREFIX = "user:extraInfo:"
    }

//    fun fillFieldByRedis(redisTemplate: RedisTemplate<String, Int>): Boolean
//    {
//        val map = redisTemplate.opsForHash<String, Int>().entries(USER_EXTRA_INFO_REDIS_PREFIX + userId)
//        if (map.isEmpty()) return false
//        this.fillFieldByMap(map)
//        return true
//    }
//    fun fillFieldByMap(map: Map<String, Int>)
//    {
//        val fields = this::class.java.declaredFields.filter { it.isAnnotationPresent(TableField::class.java) }
//        for (field in fields)
//        {
//            field.isAccessible = true
//            val value = map[field.name] ?: continue
//            when (field.type)
//            {
//                Integer::class.java  -> field.set(this, value)
//                Long::class.java -> field.set(this, value.toLong())
//            }
//        }
//    }
//    fun saveFieldByRedis(redisTemplate: RedisTemplate<String, Int>): Boolean
//    {
//        val hashOps = redisTemplate.opsForHash<String, Int>()
//        val redisKey = USER_EXTRA_INFO_REDIS_PREFIX + userId
//        val map = mutableMapOf<String, Int>()
//        val fields = this::class.java.declaredFields.filter { it.isAnnotationPresent(TableField::class.java) }
//        for (field in fields)
//        {
//            field.isAccessible = true
//            val value = field.get(this) ?: continue
//            if(value is Int)
//            {
//                map[field.name] = value
//            }
//        }
//        if (map.isNotEmpty())
//        {
//            hashOps.putAll(redisKey, map)
//        }
//        return true
//    }


}
