package com.cainsgl.user.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.user.UserLogEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface UserLogMapper : BaseMapper<UserLogEntity>
