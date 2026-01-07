package com.cainsgl.article.system

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.article.system.entity.CarouselEntity
import com.cainsgl.article.system.repository.CarouselMapper
import com.cainsgl.common.util.FineLockCacheUtils.getWithFineLock
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class CarouselServiceImpl : ServiceImpl<CarouselMapper, CarouselEntity>(), IService<CarouselEntity>
{
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, List<CarouselEntity>>

    companion object
    {
        val CAROUSEL_REDIS_PREFIX_KEY = "carousel:1"
    }

    fun getCarousels(): List<CarouselEntity>
    {
       return redisTemplate.getWithFineLock(CAROUSEL_REDIS_PREFIX_KEY, { t ->
            val zoneId = ZoneId.of("Asia/Shanghai")
            val now = LocalDateTime.now(zoneId)
            val tomorrow6Am = now.plusDays(1)
                .withHour(6)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            return@getWithFineLock Duration.between(now, tomorrow6Am)
        }, {
            val query=QueryWrapper<CarouselEntity>().orderByDesc("date").last("limit 9")
            return@getWithFineLock baseMapper.selectList(query)
        })?: emptyList()
    }
}