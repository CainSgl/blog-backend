package com.cainsgl.api.user.follow

interface UserFollowService {
    fun hasFollow(followerId: Long, followeeId: Long): Boolean
}