package com.cainsgl.article.api

import com.cainsgl.article.service.PostChunkVectorServiceImpl
import com.cainsgl.grpc.article.*
import io.grpc.stub.StreamObserver
import jakarta.annotation.Resource
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class PostChunkVectorServiceGrpcImpl : PostChunkVectorServiceGrpc.PostChunkVectorServiceImplBase()
{
    @Resource
    lateinit var postChunkVectorService: PostChunkVectorServiceImpl

    override fun reloadVector(request: ReloadVectorRequest, responseObserver: StreamObserver<ReloadVectorResponse>)
    {
        val originContent = if (request.hasOriginContent()) request.originContent else null
        val success = postChunkVectorService.reloadVector(request.postId, originContent)
        val response = ReloadVectorResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun removeVector(request: RemoveVectorRequest, responseObserver: StreamObserver<RemoveVectorResponse>)
    {
        val success = postChunkVectorService.removeVector(request.postId)
        val response = RemoveVectorResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
