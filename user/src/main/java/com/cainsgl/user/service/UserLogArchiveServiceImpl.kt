package com.cainsgl.user.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.user.UserLogArchiveEntity
import com.cainsgl.user.repository.UserLogArchiveMapper
import org.springframework.stereotype.Service

@Service
class UserLogArchiveServiceImpl : ServiceImpl<UserLogArchiveMapper, UserLogArchiveEntity>(), IService<UserLogArchiveEntity>
{
}
