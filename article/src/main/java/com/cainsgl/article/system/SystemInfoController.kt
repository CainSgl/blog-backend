package com.cainsgl.article.system

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.article.system.entity.CarouselEntity
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.common.util.FineLockCacheUtils.getWithFineLock
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/system")
class SystemInfoController
{
    @Resource
    lateinit var carouselServiceImpl: CarouselServiceImpl
    @Resource
    lateinit var postService: PostServiceImpl
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, List<PostEntity>>
    @GetMapping("/carousel")
    fun getCarousel():List<CarouselEntity>
    {
        //去读redis的缓存，并且缓存时间必定是一天
        return carouselServiceImpl.getCarousels()
    }
    @GetMapping("/announcement")
    fun getAnnouncement():List<PostEntity>
    {
        return redisTemplate.getWithFineLock("announcement",{s-> Duration.ofHours(24)}){
            val query=QueryWrapper<PostEntity>().select(PostEntity.BASIC_COL).eq("kb_id",1)
            return@getWithFineLock postService.list(query)
        }?:emptyList()
    }

}