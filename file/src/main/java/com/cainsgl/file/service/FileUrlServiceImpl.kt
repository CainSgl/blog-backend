package com.cainsgl.file.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.user.UserService
import com.cainsgl.common.entity.file.FileUrlEntity
import com.cainsgl.common.util.FineLockCacheUtils.withFineLockByDoubleChecked
import com.cainsgl.common.util.HotKeyValidator
import com.cainsgl.file.FileService
import com.cainsgl.file.repository.FileUrlMapper
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration

@Service
class FileUrlServiceImpl : ServiceImpl<FileUrlMapper, FileUrlEntity>(), IService<FileUrlEntity>
{
    companion object
    {
        const val FILE_REDIS_PRE_FIX = "file:"
    }
    @Resource
    private lateinit var transactionTemplate: TransactionTemplate

    @Resource
    lateinit var userService: UserService
    @Resource
    lateinit var hotKeyValidator: HotKeyValidator
    
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, FileUrlEntity>
    @Resource
    lateinit var fileService: FileService
    fun getById(id: Long): FileUrlEntity?
    {
        if (id < 0)
        {
            return null
        }
        val key = "$FILE_REDIS_PRE_FIX$id"
        val data = redisTemplate.opsForValue().get(key)
        if (data != null)
        {
            return data
        }
        if (hotKeyValidator.isHotKey(key))
        {
            return redisTemplate.withFineLockByDoubleChecked(key, { Duration.ofMinutes(10) }) {
                return@withFineLockByDoubleChecked super<ServiceImpl>.getById(id)
            }
        } else
        {
            return super<ServiceImpl>.getById(id)
        }
    }
    /**
     * 删除文件的内部实现
     * 处理引用计数和实际文件删除
     */
    fun deleteFileInternal(files: List<FileUrlEntity>, userId: Long)
    {
        val urlToFilesMap = files.groupBy { it.url }
        val totalSize = files.sumOf { it.fileSize ?: 0 }

        transactionTemplate.execute {
            urlToFilesMap.forEach { (sha256Hash, groupFiles) ->
                if (sha256Hash.isNullOrBlank()) return@forEach

                val currentDeleteCount = groupFiles.size
                // 使用自定义方法查询引用计数，正确处理 bytea 类型
                val totalReferences = baseMapper.countByUrl(sha256Hash)

                // 删除数据库记录
                groupFiles.forEach { file -> removeById(file) }

                // 如果没有其他引用，删除实际文件
                if (totalReferences <= currentDeleteCount)
                {
                    val extension = groupFiles.firstOrNull()?.name?.substringAfterLast(".", "") ?: ""
                    fileService.delete(sha256Hash, extension)
                }
            }

            // 释放用户存储空间
            userService.mallocMemory(userId, -totalSize)
        }
    }
}