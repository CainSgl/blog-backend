package com.cainsgl.api.user.follow

import com.cainsgl.grpc.user.HasFollowRequest
import com.cainsgl.grpc.user.UserFollowServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.user.service.UserFollowServiceImpl"])
class UserFollowGrpcService : UserFollowService {
    @GrpcClient("UserFollowService")
    lateinit var userFollowServiceGrpc: UserFollowServiceGrpc.UserFollowServiceBlockingStub

    override fun hasFollow(followerId: Long, followeeId: Long): Boolean {
        val request = HasFollowRequest.newBuilder()
            .setFollowerId(followerId)
            .setFolloweeId(followeeId)
            .build()
        val response = userFollowServiceGrpc.hasFollow(request)
        return response.hasFollow
    }
}