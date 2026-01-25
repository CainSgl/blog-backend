package com.cainsgl.article.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.post.PostService
import com.cainsgl.article.repository.PostMapper
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.common.util.FineLockCacheUtils.getWithFineLock
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class PostServiceImpl : ServiceImpl<PostMapper, PostEntity>(), PostService, IService<PostEntity>
{
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, PostEntity>

    companion object
    {
        const val POST_INFO_REDIS_PREFIX = "post:"
    }

    /**
     * TODO，后续降级，不是所有都应该缓存到redis里的
     */
    fun getPost(id: Long): PostEntity?
    {
        //使用双重检查锁
        if (id < 0)
        {
            return null
        }
        //从redis里尝试获取
        val postEntity = redisTemplate.opsForValue().get("$POST_INFO_REDIS_PREFIX$id")
        if (postEntity != null)
        {
            redisTemplate.expire("$POST_INFO_REDIS_PREFIX$id", Duration.ofMinutes(20))
            return postEntity
        }
        //TODO 后续优化
        synchronized(this) {
            val postEntity2 = redisTemplate.opsForValue().get("$POST_INFO_REDIS_PREFIX$id")
            if (postEntity2 != null)
            {
                return postEntity2
            }
            val entity = super<ServiceImpl>.getById(id) ?: return null
            if (entity.status == ArticleStatus.PUBLISHED || entity.status == ArticleStatus.ONLY_FANS)
            {
                //只缓存发布的文章
                redisTemplate.opsForValue().setIfAbsent("$POST_INFO_REDIS_PREFIX$id", entity, Duration.ofMinutes(10))
            }
            return entity
        }
    }

    fun removeCache(id: Long)
    {
        redisTemplate.delete("$POST_INFO_REDIS_PREFIX$id")
    }

    fun cursor(lastUpdatedAt: LocalDateTime?, lastLikeRatio: Double?, lastId: Long?, pageSize: Int): List<PostEntity>
    {
        if (lastUpdatedAt == null || lastLikeRatio == null || lastId == null)
        {
            return baseMapper.selectFirstPage(pageSize)
        }
        return baseMapper.selectPostsByCursor(lastUpdatedAt, lastLikeRatio, lastId, pageSize)
    }
    fun similarPost(id: Long): List<PostEntity>
    {
        //由于这里性能损耗比较大，直接缓存
        val redisTemplate2 =redisTemplate as RedisTemplate<String, List<PostEntity>>
        return  redisTemplate2.getWithFineLock("similar:$POST_INFO_REDIS_PREFIX$id", Duration.ofMinutes(20)){
          return@getWithFineLock  baseMapper.selectSimilarPostsByVector(id,10)
        }?: emptyList()
    }
    fun getPostBySimilarVector(embedding: FloatArray): List<PostEntity>
    {
        return baseMapper.selectPostsByVector(embedding,10)
    }



    override fun getById(id: Long): PostEntity?
    {
        return baseMapper.selectById(id)
    }

    override fun getByIds(ids: List<Long>): List<PostEntity>
    {
        if (ids.isEmpty()) return emptyList()
        return listByIds(ids)
    }

    override fun getVectorById(id: Long): FloatArray?
    {
        //去数据库查
        val queryWrapper = QueryWrapper<PostEntity>()
        queryWrapper.select("vector")
        queryWrapper.eq("id", id)
        val entity = baseMapper.selectOne(queryWrapper)
        return entity.vecotr
        //    return baseMapper.selectVectorById(id)
    }


    override fun addViewCount(id: Long, count: Int): Boolean
    {
        val wrapper = UpdateWrapper<PostEntity>()
        wrapper.eq("id", id)
        wrapper.setSql("viewCount = viewCount + $count")
        return baseMapper.update(wrapper) > 0
    }

    override fun addCommentCount(id: Long, count: Int): Boolean
    {
        val wrapper = UpdateWrapper<PostEntity>()
        wrapper.eq("id", id)
        wrapper.setSql("comment_count = comment_count + $count")
        return baseMapper.update(wrapper) > 0
    }
    fun addLikeCount(id: Long, count: Int): Boolean
    {
        val wrapper = UpdateWrapper<PostEntity>()
        wrapper.eq("id", id)
        wrapper.setSql("like_count = like_count + $count")
        return baseMapper.update(wrapper) > 0
    }
    fun addStarCount(id: Long, count: Int): Boolean
    {
        val wrapper = UpdateWrapper<PostEntity>()
        wrapper.eq("id", id)
        wrapper.setSql("star_count = star_count + $count")
        return baseMapper.update(wrapper) > 0
    }
}
