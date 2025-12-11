package com.cainsgl.user.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.user.log.UserLogService
import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.user.log.LogDispatcher
import com.cainsgl.user.repository.UserLogMapper
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
        list.forEach { logDispatcher.dispatch(it) }
        //归档到旧数据
        return list.size
    }
}
