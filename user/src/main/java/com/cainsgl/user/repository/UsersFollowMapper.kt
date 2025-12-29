package com.cainsgl.user.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.user.UsersFollowEntity
import com.cainsgl.user.dto.response.FollowUserResponse
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface UsersFollowMapper : BaseMapper<UsersFollowEntity>
{

    
    fun checkFollowing(followerId: Long, followeeId: Long): Boolean
    
    fun getFollowerUsers(@Param("userId") userId: Long, @Param("lastId") lastId: Long): List<FollowUserResponse>
    
    fun getFolloweeUsers(@Param("userId") userId: Long, @Param("lastId") lastId: Long): List<FollowUserResponse>
}