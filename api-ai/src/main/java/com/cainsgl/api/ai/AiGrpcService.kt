package com.cainsgl.api.ai

import com.cainsgl.grpc.ai.AiServiceGrpc
import com.cainsgl.grpc.ai.BatchEmbeddingRequest
import com.cainsgl.grpc.ai.EmbeddingRequest
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.ai.service.AiServiceImpl"])
class AiGrpcService : AiService
{
    @GrpcClient("AiService")
    lateinit var aiServiceGrpc: AiServiceGrpc.AiServiceBlockingStub

    override fun getEmbedding(text: String): FloatArray
    {
        val request = EmbeddingRequest.newBuilder()
            .setText(text)
            .build()
        val response = aiServiceGrpc.getEmbedding(request)
        return response.embeddingList.toFloatArray()
    }

    override fun getEmbedding(texts: List<String>): List<FloatArray>
    {
        val request = BatchEmbeddingRequest.newBuilder()
            .addAllTexts(texts)
            .build()
        val response = aiServiceGrpc.getBatchEmbedding(request)
        return response.embeddingsList.map { it.embeddingList.toFloatArray() }
    }
}

