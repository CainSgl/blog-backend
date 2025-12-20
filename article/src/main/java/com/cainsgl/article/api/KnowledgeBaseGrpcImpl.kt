package com.cainsgl.article.api

import com.cainsgl.article.service.KnowledgeBaseServiceImpl
import com.cainsgl.grpc.article.AddKbLikeCountRequest
import com.cainsgl.grpc.article.KnowledgeBaseServiceGrpc
import com.cainsgl.grpc.article.RemoveVectorResponse
import io.grpc.stub.StreamObserver
import jakarta.annotation.Resource
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class KnowledgeBaseGrpcImpl : KnowledgeBaseServiceGrpc.KnowledgeBaseServiceImplBase() {
    @Resource
    lateinit var knowledgeBaseService: KnowledgeBaseServiceImpl

    override fun addKbLikeCount(request: AddKbLikeCountRequest, responseObserver: StreamObserver<RemoveVectorResponse>) {
        // 实现点赞计数的增加
        knowledgeBaseService.addKbLikeCount(request.kbId, request.addCount)
        val response = RemoveVectorResponse.newBuilder().setSuccess(true).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}