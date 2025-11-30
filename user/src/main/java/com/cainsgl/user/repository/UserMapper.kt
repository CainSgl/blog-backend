package com.cainsgl.user.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.user.UserEntity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface UserMapper : BaseMapper<UserEntity>
{

    /**
     * 获取用户的extra字段
     */
    fun selectExtraById(@Param("id") id: Long): String?
}
