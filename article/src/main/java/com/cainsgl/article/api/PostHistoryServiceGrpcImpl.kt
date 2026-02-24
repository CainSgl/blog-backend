package com.cainsgl.article.api

import com.cainsgl.article.service.PostHistoryServiceImpl
import com.cainsgl.common.entity.article.PostHistoryEntity
import com.cainsgl.grpc.article.GetPostHistoryByIdRequest
import com.cainsgl.grpc.article.GetPostHistoryByIdResponse
import com.cainsgl.grpc.article.PostHistoryServiceGrpc
import io.grpc.stub.StreamObserver
import jakarta.annotation.Resource
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class PostHistoryServiceGrpcImpl : PostHistoryServiceGrpc.PostHistoryServiceImplBase()
{
    @Resource
    lateinit var postHistoryService: PostHistoryServiceImpl

    override fun getLastById(request: GetPostHistoryByIdRequest, responseObserver: StreamObserver<GetPostHistoryByIdResponse>)
    {
        val postHistory = postHistoryService.getLastById(request.postId)
        val response = if (postHistory != null)
        {
            GetPostHistoryByIdResponse.newBuilder()
                .setExists(true)
                .setPostHistory(postHistory.toProto())
                .build()
        } else
        {
            GetPostHistoryByIdResponse.newBuilder()
                .setExists(false)
                .build()
        }
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun updateById(request: com.cainsgl.grpc.article.UpdateByIdRequest, responseObserver: StreamObserver<com.cainsgl.grpc.article.RemoveVectorResponse>)
    {
        val success = postHistoryService.updateById(request.historyId, request.content)
        val response = com.cainsgl.grpc.article.RemoveVectorResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun createNewVersion(request: com.cainsgl.grpc.article.CreateNewVersionRequest, responseObserver: StreamObserver<com.cainsgl.grpc.article.RemoveVectorResponse>)
    {
        val success = postHistoryService.createNewVersion(
            userId = request.userId,
            postId = request.postId,
            version = request.version,
            content = request.content
        )
        val response = com.cainsgl.grpc.article.RemoveVectorResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    private fun PostHistoryEntity.toProto(): com.cainsgl.grpc.article.PostHistoryEntity
    {
        return com.cainsgl.grpc.article.PostHistoryEntity.newBuilder()
            .setId(this.id ?: -1)
            .setUserId(this.userId ?: -1)
            .setPostId(this.postId ?: -1)
            .setContent(this.content ?: "")
            .setCreatedAt(this.createdAt?.toString())
            .setVersion(this.version ?: -1)
            .build()
    }
}