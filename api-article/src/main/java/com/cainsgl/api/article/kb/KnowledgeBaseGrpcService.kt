package com.cainsgl.api.article.kb

import com.cainsgl.grpc.article.AddKbLikeCountRequest
import com.cainsgl.grpc.article.KnowledgeBaseServiceGrpc
import com.cainsgl.grpc.article.RemoveVectorResponse
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.KnowledgeBaseServiceImpl"])
class KnowledgeBaseGrpcService : KnowledgeBaseService
{
    @GrpcClient("KnowledgeBaseService")
    lateinit var knowledgeBaseServiceGrpc: KnowledgeBaseServiceGrpc.KnowledgeBaseServiceBlockingStub

    override fun addKbLikeCount(kbId: Long, addCount: Int) {
        val request = AddKbLikeCountRequest.newBuilder()
            .setKbId(kbId)
            .setAddCount(addCount)
            .build()
        knowledgeBaseServiceGrpc.addKbLikeCount(request)
    }
}
