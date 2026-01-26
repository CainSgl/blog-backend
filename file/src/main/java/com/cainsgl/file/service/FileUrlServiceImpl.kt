package com.cainsgl.file.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.file.FileUrlEntity
import com.cainsgl.common.util.FineLockCacheUtils.withFineLockByDoubleChecked
import com.cainsgl.common.util.HotKeyValidator
import com.cainsgl.file.repository.FileUrlMapper
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class FileUrlServiceImpl : ServiceImpl<FileUrlMapper, FileUrlEntity>(), IService<FileUrlEntity>
{
    companion object
    {
        val FILE_REDIS_PRE_FIX = "file:"
    }
    @Resource
    lateinit var hotKeyValidator: HotKeyValidator
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, FileUrlEntity>
    fun getById(id: Long): FileUrlEntity?
    {
        if (id < 0)
        {
            return null
        }
        val key = "$FILE_REDIS_PRE_FIX$id"
        val data =redisTemplate.opsForValue().get(key)
        if(data != null)
        {
            return data
        }
        if(hotKeyValidator.isHotKey(key))
        {
            return redisTemplate.withFineLockByDoubleChecked(key, { Duration.ofMinutes(30) }) {
                return@withFineLockByDoubleChecked super<ServiceImpl>.getById(id)
            }
        }else
        {
            return super<ServiceImpl>.getById(id)
        }

    }

}