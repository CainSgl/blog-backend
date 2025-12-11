package com.cainsgl.user.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.user.extra.UserExtraInfoService
import com.cainsgl.common.entity.user.UserExtraInfoEntity
import com.cainsgl.user.repository.UserExtraInfoMapper
import org.springframework.stereotype.Service

@Service
class UserExtraInfoServiceImpl : ServiceImpl<UserExtraInfoMapper, UserExtraInfoEntity>(), UserExtraInfoService, IService<UserExtraInfoEntity>
{
    override fun getInterestVector(userId: Long): FloatArray?
    {
        return getById(userId)?.interestVector
    }
}
