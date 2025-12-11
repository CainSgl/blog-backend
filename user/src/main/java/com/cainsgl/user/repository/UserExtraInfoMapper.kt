package com.cainsgl.user.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.user.UserExtraInfoEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface UserExtraInfoMapper : BaseMapper<UserExtraInfoEntity>
