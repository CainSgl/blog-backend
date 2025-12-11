package com.cainsgl.user.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.user.UserLogArchiveEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface UserLogArchiveMapper : BaseMapper<UserLogArchiveEntity>
