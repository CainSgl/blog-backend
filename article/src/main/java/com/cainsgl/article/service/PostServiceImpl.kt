package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.post.PostService
import com.cainsgl.article.repository.PostMapper
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.common.util.FineLockCacheUtils.getWithFineLock
import com.cainsgl.common.util.FineLockCacheUtils.withFineLockByDoubleChecked
import com.cainsgl.common.util.HotKeyValidator
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

    @Resource
    lateinit var redisTemplateStr: RedisTemplate<String, String>

    @Resource
    lateinit var hotKeyValidator: HotKeyValidator

    companion object
    {
        const val POST_INFO_REDIS_PREFIX = "post:"
        //TODO 后续需要写回数据库
        const val POST_COUNT_INFO_REDIS_PREFIX = "cursor:postcount:"
    }


    fun getPostBaseInfo(id: Long): PostEntity?
    {
        val key = "$POST_INFO_REDIS_PREFIX$id"
        //使用双重检查锁
        if (id < 0)
        {
            return null
        }
        //从redis里尝试获取
        val postEntity = redisTemplate.opsForValue().get(key)
        if (postEntity != null)
        {
            return postEntity
        }
        if (hotKeyValidator.isHotKey(key))
        {
            synchronized(this) {
                val postEntity2 = redisTemplate.opsForValue().get(key)
                if (postEntity2 != null)
                {
                    return postEntity2
                }
                val entity = super<ServiceImpl>.getById(id) ?: return null
                if (entity.status == ArticleStatus.PUBLISHED || entity.status == ArticleStatus.ONLY_FANS)
                {
                    //只缓存发布的文章
                    redisTemplate.opsForValue().setIfAbsent(key, entity, Duration.ofMinutes(10))
                }
                return entity
            }
        } else
        {
            return super<ServiceImpl>.getById(id)
        }
    }

    fun getPostContent(id: Long): String?
    {

        val queryWrapper = KtQueryWrapper(PostEntity::class.java).select(PostEntity::content)
        queryWrapper.eq(PostEntity::id, id)
        return super<ServiceImpl>.getOne(queryWrapper).content
    }

    fun getContentByEntity(entity: PostEntity): PostEntity
    {
        if (entity.id == null)
        {
            entity.content = "无法获取内容，这可能是服务器异常！请检查id"
            return entity
        }
        val key = "${POST_INFO_REDIS_PREFIX}content:${entity.id}"

        val content = redisTemplateStr.opsForValue().get(key)
        if (content != null)
        {
            entity.content = content
            return entity
        }
        if (hotKeyValidator.isHotKey(key, count = 9))
        {
            entity.content = redisTemplateStr.withFineLockByDoubleChecked(key, { Duration.ofMinutes(10) }) {
                getPostContent(entity.id!!)
            }
            return entity
        } else
        {
            entity.content = getPostContent(entity.id!!)
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
        val redisTemplate2 = redisTemplate as RedisTemplate<String, List<PostEntity>>
        return redisTemplate2.getWithFineLock("similar:$POST_INFO_REDIS_PREFIX$id", Duration.ofMinutes(20)) {
            return@getWithFineLock baseMapper.selectSimilarPostsByVector(id, 10)
        } ?: emptyList()
    }

    fun getPostBySimilarVector(embedding: FloatArray): List<PostEntity>
    {
        return baseMapper.selectPostsByVector(embedding, 10)
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
        val queryWrapper = KtQueryWrapper(PostEntity::class.java)
        queryWrapper.select(PostEntity::vecotr)
        queryWrapper.eq(PostEntity::id, id)
        val entity = baseMapper.selectOne(queryWrapper)
        return entity.vecotr
        //    return baseMapper.selectVectorById(id)
    }

    /**
     * 增加阅读数量和评论数量这些，可能会造成严重的写冲突锁，这里直接去写入redis等定时任务刷入数据库
     */

    override fun addViewCount(id: Long, count: Int): Boolean
    {
        //这个是兼容老api，这里不再做修改
        return addCount(id, count.toLong(), "view")
    }

    override fun addCommentCount(id: Long, count: Int): Boolean
    {
//        val wrapper = UpdateWrapper<PostEntity>()
//        wrapper.eq("id", id)
//        wrapper.setSql("comment_count = comment_count + $count")
//        return baseMapper.update(wrapper) > 0
        return addCount(id, count.toLong(), "comment")
    }

    fun addLikeCount(id: Long, count: Long): Boolean
    {
        return addCount(id, count, "like")
    }

    fun addStarCount(id: Long, count: Long): Boolean
    {
        return addCount(id, count, "star")
    }

    fun addCount(id: Long, count: Long, type: String): Boolean
    {
        try
        {
            val key="${POST_COUNT_INFO_REDIS_PREFIX}${id}"
            val opsForHash = redisTemplateStr.opsForHash<String,Long>()
            opsForHash.increment(key,type, count)
            return true
        } catch (e: Exception)
        {
            log.error("无法连接到redis", e)
            //兜底
            val wrapper = KtUpdateWrapper(PostEntity::class.java)
            wrapper.eq(PostEntity::id, id)
            val field="${type}_count"
            wrapper.setSql("$field = $field + $count")
            return baseMapper.update(wrapper) > 0
        }

    }

}
