package com.cainsgl.user.api

import com.cainsgl.common.entity.user.UserExtraInfoEntity
import com.cainsgl.grpc.user.*
import com.cainsgl.user.service.UserExtraInfoServiceImpl
import io.grpc.stub.StreamObserver
import jakarta.annotation.Resource
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class UserExtraInfoServiceGrpcImpl : UserExtraInfoServiceGrpc.UserExtraInfoServiceImplBase()
{
    @Resource
    lateinit var userExtraInfoService: UserExtraInfoServiceImpl

    override fun getInterestVector(request: GetInterestVectorRequest, responseObserver: StreamObserver<GetInterestVectorResponse>)
    {
        val interestVector = userExtraInfoService.getInterestVector(request.userId)
        val response = GetInterestVectorResponse.newBuilder()
            .addAllInterestVector(interestVector?.toList() ?: emptyList())
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun setInterestVector(request: SetInterestVectorRequest, responseObserver: StreamObserver<SetInterestVectorResponse>)
    {
        val success =
            userExtraInfoService.setInterestVector(request.userId, request.interestVectorList.toFloatArray())
        val response = SetInterestVectorResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun saveCount(request: SaveCountRequest, responseObserver: StreamObserver<SaveCountResponse>)
    {
        val userExtraInfo = UserExtraInfoEntity(
            userId = request.userId,
            likeCount = if (request.hasLikeCount()) request.likeCount else null,
            commentCount = if (request.hasCommentCount()) request.commentCount else null,
            postCount = if (request.hasPostCount()) request.postCount else null,
            articleViewCount = if (request.hasArticleViewCount()) request.articleViewCount else null,
            followingCount = if (request.hasFollowingCount()) request.followingCount else null,
            followerCount = if (request.hasFollowerCount()) request.followerCount else null
        )
        
        val success = userExtraInfoService.saveCount(userExtraInfo)
        val response = SaveCountResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
