package com.cainsgl.user.api

import com.cainsgl.grpc.user.*
import com.cainsgl.user.service.UserFollowServiceImpl
import io.grpc.stub.StreamObserver
import jakarta.annotation.Resource
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class UserFollowServiceGrpcImpl : UserFollowServiceGrpc.UserFollowServiceImplBase() {
    @Resource
    lateinit var userFollowService: UserFollowServiceImpl

    override fun hasFollow(request: HasFollowRequest, responseObserver: StreamObserver<HasFollowResponse>) {
        val hasFollow = userFollowService.checkFollowing(request.followerId, request.followeeId)
        val response = HasFollowResponse.newBuilder()
            .setHasFollow(hasFollow)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}