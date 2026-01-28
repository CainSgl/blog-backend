package com.cainsgl.comment.service

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.comment.entity.ParagraphEntity
import com.cainsgl.comment.repository.ParagraphMapper
import com.cainsgl.common.util.HotKeyValidator
import jakarta.annotation.Resource
import org.redisson.api.RedissonClient
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class ParagraphServiceImpl : ServiceImpl<ParagraphMapper, ParagraphEntity>()
{
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @Resource
    lateinit var redissonClient: RedissonClient

    @Resource
    lateinit var hotKeyValidator: HotKeyValidator

    companion object
    {
        const val PARAGRAPH_REDIS_PREFIX_KEY = "paragraph:"
        const val PARAGRAPH_COUNT_INFO = "cursor:paragraph_count:"
    }

    /**
     * 只有hotkey才会加载到redis里去
     */
    fun getCountByPost(id: Long, version: Int): List<ParagraphEntity>?
    {
        val redisKey = "$PARAGRAPH_REDIS_PREFIX_KEY$id:$version"
        fun getDataByRedis(): List<ParagraphEntity>?
        {
            // 尝试从 Redis Hash 获取所有字段
            val hashOps = redisTemplate.opsForHash<String, Int>()
            val entries = hashOps.entries(redisKey)

            if (entries.isNotEmpty())
            {
                // 如果 Redis 中有数据，构建 List<ParagraphEntity> 并返回
                return entries.map { (dataId, count) ->
                    ParagraphEntity(
                        dataId = dataId.toInt(),
                        count = count,
                    )
                }
            }
            return null
        }

        fun getDataByDB(): List<ParagraphEntity>
        {
            val query =
                KtQueryWrapper(ParagraphEntity::class.java).select(ParagraphEntity::dataId,ParagraphEntity::count).eq(ParagraphEntity::postId, id).eq(ParagraphEntity::version, version)
            val entities = baseMapper.selectList(query)
            return entities
        }

        val data = getDataByRedis()
        if (data != null)
        {
            return data
        }
        if (hotKeyValidator.isHotKey(redisKey))
        {
            val lock = redissonClient.getLock("lock:$redisKey")
            val isLockAcquired = lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)
            if (isLockAcquired)
            {
                try
                {
                    val data2 = getDataByRedis()
                    if (data2 != null)
                    {
                        return data2
                    }
                    // 将数据按 data_id 分组并存储到 Redis Hash
                    val hashOps = redisTemplate.opsForHash<String, Int>()
                    val entities = getDataByDB()
                    if (entities.isNotEmpty())
                    {
                        val hashData = mutableMapOf<String, Int>()
                        entities.forEach { entity ->
                            entity.dataId?.let { dataId ->
                                hashData[dataId.toString()] = entity.count ?: -1
                            }
                        }
                        hashOps.putAll(redisKey, hashData)
                        redisTemplate.expire(redisKey, java.time.Duration.ofMinutes(10))
                    }
                    return entities
                } finally
                {
                    lock.unlock()
                }
            } else
            {
                //备用方案，直接读数据库
                return getDataByDB()
            }
        } else
        {
            return getDataByDB()
        }


    }

    /**
     * 自增指定 postId、version、dataId 的计数
     */
    fun incrementCount(postId: Long, version: Int, dataId: Int,value:Long=1): Boolean
    {

        val hashKey = dataId.toString()
        try
        {
            val redisKey = "$PARAGRAPH_COUNT_INFO$postId:$version"
            val hashOps = redisTemplate.opsForHash<String, Int>()
            hashOps.increment(redisKey, hashKey, 1L)
        } catch (e: Exception)
        {
            log.error("似乎是无法连接到redis，采用兜底方案", e)
            val update =
                KtUpdateWrapper(ParagraphEntity::class.java).eq(ParagraphEntity::postId, postId).eq(ParagraphEntity::dataId, dataId).eq(ParagraphEntity::version, version)
                    .setSql("count = count+1")
            this.update(update)
        } finally
        {
            //这里是去临时的增加显示给用户的信息，上面的是用来给定时任务刷回数据库的。
            val redisKey = "$PARAGRAPH_REDIS_PREFIX_KEY$postId:$version"
            val hasKey = redisTemplate.hasKey(redisKey)
            if(hasKey!=null&&hasKey){
                //这里，只有在redis里的时候，才会去自增redis里的数据，因为他可能不是热点数据
                val hashOps = redisTemplate.opsForHash<String, Int>()
                hashOps.increment(redisKey, hashKey, value)
            }
        }
        return true
    }

    /**
     * 设置指定 postId、version、dataId 的计数值，只有首个创建段落评论才会调用
     */
    fun addCount(postId: Long, version: Int, dataId: Int, value: Long)
    {
        //需要注意在不在redis里存在！存在才去自增，不然说明不是热点数据，直接去增加数据库的（controller层保存的时候完成）
        val redisKey = "$PARAGRAPH_REDIS_PREFIX_KEY$postId:$version"
        val hasKey = redisTemplate.hasKey(redisKey)
        if(hasKey!=null&&hasKey){
            val hashOps = redisTemplate.opsForHash<String, Int>()
            hashOps.increment(redisKey,  dataId.toString(), value)
        }

    }


}