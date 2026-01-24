package com.cainsgl.api.article.kb

import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.grpc.article.AddKbLikeCountRequest
import com.cainsgl.grpc.article.GetKbsByIdsRequest
import com.cainsgl.grpc.article.KnowledgeBaseServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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

    override fun getByIds(ids: List<Long>): List<com.cainsgl.common.entity.article.KnowledgeBaseEntity> {
        val request = GetKbsByIdsRequest.newBuilder()
            .addAllIds(ids)
            .build()
        val response = knowledgeBaseServiceGrpc.getByIds(request)
        return response.kbsList.map { it.toEntity() }
    }

    private fun com.cainsgl.grpc.article.KnowledgeBaseEntity.toEntity(): com.cainsgl.common.entity.article.KnowledgeBaseEntity {
        return com.cainsgl.common.entity.article.KnowledgeBaseEntity(
            id = this.id,
            userId = this.userId,
            name = this.name,
            createdAt = this.createdAt.takeIf { it.isNotEmpty() }?.let { LocalDateTime.parse(it) },
            status = ArticleStatus.fromNumber(this.status.number),
            index = this.index,
            likeCount = this.likeCount
        )
    }
}
