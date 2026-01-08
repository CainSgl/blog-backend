package com.cainsgl.comment.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.comment.entity.ParagraphEntity
import com.cainsgl.comment.repository.ParagraphMapper
import jakarta.annotation.Resource
import org.redisson.api.RedissonClient
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ParagraphServiceImpl : ServiceImpl<ParagraphMapper, ParagraphEntity>()
{
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>
    @Resource
    lateinit var redissonClient: RedissonClient
    companion object
    {
        //TODO，后续需要扫描key，然后写回count
        const val PARAGRAPH_REDIS_PREFIX_KEY = "paragraph:"
    }


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
                QueryWrapper<ParagraphEntity>().select("data_id", "count").eq("post_id", id).eq("version", version)
            val entities = baseMapper.selectList(query)
            return entities
        }
        val data = getDataByRedis()
        if (data != null)
        {
            return data
        }
        val lock = redissonClient.getLock("lock:"+redisKey)
        val isLockAcquired = lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)
        if (isLockAcquired)
        {
           try{
               val data2 = getDataByRedis()
               if (data2 != null)
               {
                   return data2
               }
               // 将数据按 data_id 分组并存储到 Redis Hash
               val hashOps = redisTemplate.opsForHash<String, Int>()
               val entities= getDataByDB()
               if (entities.isNotEmpty())
               {
                   val hashData = mutableMapOf<String, Int>()
                   entities.forEach { entity ->
                       entity.dataId?.let { dataId ->
                           hashData[dataId.toString()] = entity.count ?: -1
                       }
                   }
                   hashOps.putAll(redisKey, hashData)
               } else
               {
                   //防止缓存穿透
                   hashOps.put(redisKey, "-1", 0)
               }
               return entities
           }finally
           {
               lock.unlock()
           }
        }else
        {
            //备用方案，直接读数据库
            return getDataByDB()
        }

    }

    /**
     * 自增指定 postId、version、dataId 的计数
     * 验证 key 和 field 存在，否则抛出异常
     */
    @Transactional
    fun incrementCount(postId: Long, version: Int, dataId: Int): Boolean
    {
        val redisKey = "$PARAGRAPH_REDIS_PREFIX_KEY$postId:$version"
        val hashKey = dataId.toString()

        // 检查 Redis Hash 中是否存在指定的 field
        val hashOps = redisTemplate.opsForHash<String, Int>()
        val hasField = hashOps.hasKey(redisKey, hashKey)
        if (!hasField)
        {
            //该情况比较极端，发生在用户阅读文章十分钟后，并且这期间没有其他用户阅读，数据已经过期
            //去数据库里，单独的把他加载进来放入内存
            //存在更极端的情况，就是放进redis的时候，有其他用户来加载数据了，所以仍然需要双重检查锁，并且由于数据是会变更的，不能用本地锁，需要使用分布式锁
            val lock = redissonClient.getLock("lock:"+redisKey)
            val isLockAcquired = lock.tryLock(8, java.util.concurrent.TimeUnit.SECONDS)
            try{
                if (isLockAcquired)
                {
                    val query =
                        QueryWrapper<ParagraphEntity>().select("data_id", "count").eq("post_id", postId).eq("data_id", dataId)
                            .eq("version", version)
                    val paragraphEntity = this.baseMapper.selectOne(query)
                    val value = paragraphEntity.count!! + 1;
                    hashOps.put(redisKey, hashKey, value)
                }else
                {
                    //极端情况，直接自增数据库的
                    val update= UpdateWrapper<ParagraphEntity>().eq("post_id", postId).eq("data_id", dataId)
                        .eq("version", version).setSql("count = count+1")
                    this.update(update)
                }
            }finally
            {
                lock.unlock()
            }
            return false
        }
        hashOps.increment(redisKey, hashKey, 1)
        return true
    }

    /**
     * 设置指定 postId、version、dataId 的计数值
     */
    fun setCount(postId: Long, version: Int, dataId: Int, value: Int)
    {
        val redisKey = "$PARAGRAPH_REDIS_PREFIX_KEY$postId:$version"
        val hashKey = dataId.toString()
        // 直接设置 Redis Hash 中的字段值
        val hashOps = redisTemplate.opsForHash<String, Int>()
        hashOps.put(redisKey, hashKey, value)
    }

}