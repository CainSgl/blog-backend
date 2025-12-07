package com.cainsgl.ai.api

import com.cainsgl.ai.service.AiServiceImpl
import com.cainsgl.grpc.ai.*
import io.grpc.stub.StreamObserver
import jakarta.annotation.Resource
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class AiServiceGrpcImpl : AiServiceGrpc.AiServiceImplBase()
{
    @Resource
    lateinit var aiService: AiServiceImpl

    override fun getEmbedding(request: EmbeddingRequest, responseObserver: StreamObserver<EmbeddingResponse>)
    {
        val embedding = aiService.getEmbedding(request.text)
        val response = EmbeddingResponse.newBuilder()
            .addAllEmbedding(embedding.toList())
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getBatchEmbedding(request: BatchEmbeddingRequest, responseObserver: StreamObserver<BatchEmbeddingResponse>)
    {
        val embeddings = aiService.getEmbedding(request.textsList)
        val embeddingResponses = embeddings.map { arr ->
            EmbeddingResponse.newBuilder()
                .addAllEmbedding(arr.toList())
                .build()
        }
        val response = BatchEmbeddingResponse.newBuilder()
            .addAllEmbeddings(embeddingResponses)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
