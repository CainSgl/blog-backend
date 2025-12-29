package com.cainsgl.article.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.post.PostService
import com.cainsgl.article.repository.PostMapper
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.PostEntity
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class PostServiceImpl : ServiceImpl<PostMapper, PostEntity>(), PostService, IService<PostEntity>
{
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, PostEntity>
    companion object{
        const val POST_INFO_REDIS_PREFIX = "post:"
    }

    /**
     * 或许源码看到这里，你会质疑为什么不用分布式锁，这里主要是因为这里只做缓存，多台机子去数据库读问题不大，无非就是多写几次
     * 而用户额外信息必须拿分布式锁是因为那是强一致性需求，里面的点赞数，被关注数是会动态更新的，如果只是本机加锁会有不一致的问题
     * 这也是这里的获取方式与那里不太相同的原因，因为不是用hash存储的，而是直接用string序列化存储的，可以直接获取
     */
    fun getPost(id: Long): PostEntity?
    {
        //使用双重检查锁
        if(id<0)
        {
            return null
        }
        //从redis里尝试获取
        val postEntity = redisTemplate.opsForValue().get("$POST_INFO_REDIS_PREFIX$id")
        if(postEntity!=null)
        {
            redisTemplate.expire("$POST_INFO_REDIS_PREFIX$id", Duration.ofMinutes(20))
            return postEntity
        }
        synchronized(this) {
            val postEntity2 = redisTemplate.opsForValue().get("$POST_INFO_REDIS_PREFIX$id")
            if(postEntity2!=null)
            {
                return postEntity2
            }
            val entity = super<ServiceImpl>.getById(id) ?: return null
            if(entity.status== ArticleStatus.PUBLISHED)
            {
                //只缓存发布的文章
                redisTemplate.opsForValue().setIfAbsent("$POST_INFO_REDIS_PREFIX$id", entity,Duration.ofMinutes(10))
            }
            return entity
        }
    }
    fun removeCache(id: Long) {
        redisTemplate.delete("$POST_INFO_REDIS_PREFIX$id")
    }
    override fun getById(id: Long): PostEntity?
    {
        return baseMapper.selectById(id)
    }

    override fun getVectorById(id: Long): FloatArray?
    {
        //去数据库查
        val queryWrapper= QueryWrapper<PostEntity>()
        queryWrapper.select("vector")
        queryWrapper.eq("id", id)
        val entity = baseMapper.selectOne(queryWrapper)
        return entity.vecotr
        //    return baseMapper.selectVectorById(id)
    }

    override fun addViewCount(id: Long, count: Int):Boolean
    {
        val wrapper = UpdateWrapper<PostEntity>()
        wrapper.eq("id", id)
        wrapper.setSql("viewCount=viewCount+$count")
        return baseMapper.update(wrapper)>0
    }

}
