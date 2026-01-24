package com.cainsgl.article.api

import com.cainsgl.article.service.KnowledgeBaseServiceImpl
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import com.cainsgl.grpc.article.AddKbLikeCountRequest
import com.cainsgl.grpc.article.GetKbsByIdsRequest
import com.cainsgl.grpc.article.GetKbsByIdsResponse
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

    override fun getByIds(request: GetKbsByIdsRequest, responseObserver: StreamObserver<GetKbsByIdsResponse>) {
        val kbs = knowledgeBaseService.getByIds(request.idsList)
        val response = GetKbsByIdsResponse.newBuilder()
            .addAllKbs(kbs.map { it.toProto() })
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    private fun KnowledgeBaseEntity.toProto(): com.cainsgl.grpc.article.KnowledgeBaseEntity {
        return com.cainsgl.grpc.article.KnowledgeBaseEntity.newBuilder()
            .setId(this.id ?: 0)
            .setUserId(this.userId ?: 0)
            .setName(this.name ?: "")
            .setCreatedAt(this.createdAt?.toString() ?: "")
            .setStatus(this.status.toProtoStatus())
            .setIndex(this.index ?: "")
            .setLikeCount(this.likeCount ?: 0)
            .build()
    }

    private fun ArticleStatus?.toProtoStatus(): com.cainsgl.grpc.article.ArticleStatus
    {
        return when (this)
        {
            ArticleStatus.DRAFT          -> com.cainsgl.grpc.article.ArticleStatus.ARTICLE_STATUS_DRAFT
            ArticleStatus.PENDING_REVIEW -> com.cainsgl.grpc.article.ArticleStatus.ARTICLE_STATUS_PENDING_REVIEW
            ArticleStatus.PUBLISHED      -> com.cainsgl.grpc.article.ArticleStatus.ARTICLE_STATUS_PUBLISHED
            ArticleStatus.OFF_SHELF      -> com.cainsgl.grpc.article.ArticleStatus.ARTICLE_STATUS_OFF_SHELF
            else                         -> com.cainsgl.grpc.article.ArticleStatus.ARTICLE_STATUS_UNSPECIFIED
        }
    }
}