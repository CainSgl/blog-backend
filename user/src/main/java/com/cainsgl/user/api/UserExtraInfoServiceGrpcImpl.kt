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

    override fun saveCount(request: BatchSaveCountRequest, responseObserver: StreamObserver<SaveCountResponse>)
    {
        val userExtraInfoList = request.requestsList.map { req ->
            UserExtraInfoEntity(
                userId = req.userId,
                likeCount = if (req.hasLikeCount()) req.likeCount else null,
                commentCount = if (req.hasCommentCount()) req.commentCount else null,
                postCount = if (req.hasPostCount()) req.postCount else null,
                articleViewCount = if (req.hasArticleViewCount()) req.articleViewCount else null,
                followingCount = if (req.hasFollowingCount()) req.followingCount else null,
                followerCount = if (req.hasFollowerCount()) req.followerCount else null
            )
        }
        
        val success = userExtraInfoService.saveCount(userExtraInfoList)
        val response = SaveCountResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
