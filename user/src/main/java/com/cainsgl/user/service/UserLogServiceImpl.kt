package com.cainsgl.user.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.user.log.UserLogService
import com.cainsgl.common.entity.user.UserLogArchiveEntity
import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.user.log.LogDispatcher
import com.cainsgl.user.repository.UserLogMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional



private val logger = KotlinLogging.logger {}
@Service
class UserLogServiceImpl : ServiceImpl<UserLogMapper, UserLogEntity>(), UserLogService, IService<UserLogEntity>
{
    @Resource
    lateinit var logDispatcher: LogDispatcher
    @Resource
    lateinit var userLogArchiveService:UserLogArchiveServiceImpl

    @Transactional
    override fun processLog(value: Int): Int
    {
        //直接从userLog里获取，统计完成后再往旧数据里放
        val queryWrapper = QueryWrapper<UserLogEntity>()
        queryWrapper.last("limit $value")
        val list: List<UserLogEntity> = list(queryWrapper)
        //去批量处理
        list.forEach {
            if (!logDispatcher.dispatch(it))
            {
                //处理失败
                logger.error{"处理日志行为失败：${it.id}"}
            }
        }
        //归档到旧数据
        remove(queryWrapper)
        val list2: List<UserLogArchiveEntity> =list.map {UserLogArchiveEntity(it)}
        userLogArchiveService.saveBatch(list2)
        return list.size
    }
}
