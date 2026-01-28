package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.post.history.PostHistoryService
import com.cainsgl.article.repository.PostHistoryMapper
import com.cainsgl.common.entity.article.PostHistoryEntity
import com.cainsgl.common.util.FineLockCacheUtils.getWithFineLock
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.Collections.emptyList

@Service
class PostHistoryServiceImpl : ServiceImpl<PostHistoryMapper, PostHistoryEntity>(), PostHistoryService, IService<PostHistoryEntity> {
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, List<PostHistoryEntity>>
    companion object{
        val HISTORY_REDIS_PREFIX_KEY="post:history:"
    }
    /**
     * 实际上是返回倒数第二个，因为最新版本是给作者缓存用的
     */
    override fun getLastById(postId: Long): PostHistoryEntity?
    {
        val historyQuery = KtQueryWrapper(PostHistoryEntity::class.java)
            .eq(PostHistoryEntity::postId, postId).orderByDesc(PostHistoryEntity::version).last("LIMIT 1 OFFSET 1")
        return baseMapper.selectOne(historyQuery)
    }
    fun getByCache(postId:Long): List<PostHistoryEntity>?
    {
       return redisTemplate.getWithFineLock("$HISTORY_REDIS_PREFIX_KEY$postId",{ Duration.ofMinutes(10)},{
            val historyQuery = KtQueryWrapper(PostHistoryEntity::class.java)
                .eq(PostHistoryEntity::postId, postId).orderByDesc(PostHistoryEntity::version).last("LIMIT 10 OFFSET 1")
            return@getWithFineLock baseMapper.selectList(historyQuery)?: emptyList()
        })
    }
    fun getContentByIdAndPostIdWithNonMaxVersion(id: Long,postId: Long):String?
    {
        return baseMapper.getContentByIdAndPostIdWithNonMaxVersion(id,postId)
    }
}