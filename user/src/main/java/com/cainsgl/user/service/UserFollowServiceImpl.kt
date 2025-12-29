package com.cainsgl.user.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.user.UsersFollowEntity
import com.cainsgl.user.dto.response.FollowUserResponse
import com.cainsgl.user.repository.UsersFollowMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class UserFollowServiceImpl : ServiceImpl<UsersFollowMapper, UsersFollowEntity>()
{


    /**
     * 关注用户
     * @param followerId 这个是粉丝
     * @param followeeId 这个是关注的
     */
    @Transactional(propagation = Propagation.REQUIRED)
    fun follow(followerId: Long, followeeId: Long): Boolean
    {
        if(checkFollowing(followerId, followeeId))
        {
            //已经存在了，直接返回false
            return false
        }
        val usersFollowEntity = UsersFollowEntity(followerId = followerId, followeeId = followeeId)
        return save(usersFollowEntity)
    }

    /**
     * 取消关注
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    fun unfollow(followerId: Long, followeeId: Long): Boolean
    {
        val query = QueryWrapper<UsersFollowEntity>()
        query.eq("follower_id", followerId).eq("followee_id", followeeId)
        return remove(query)
    }

    /**
     * 检查是否已关注
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    fun checkFollowing(followerId: Long, followeeId: Long): Boolean
    {
        return this.baseMapper.checkFollowing(followerId, followeeId)
    }
    
    /**
     * 获取关注者用户基本信息
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    fun getFollowerUsers(userId: Long, lastId: Long): List<FollowUserResponse>
    {
        return this.baseMapper.getFollowerUsers(userId, lastId)
    }
    
    /**
     * 获取关注的用户基本信息
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    fun getFolloweeUsers(userId: Long, lastId: Long): List<FollowUserResponse>
    {
        return this.baseMapper.getFolloweeUsers(userId, lastId)
    }
}