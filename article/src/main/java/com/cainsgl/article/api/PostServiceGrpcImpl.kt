package com.cainsgl.article.api

import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.grpc.article.GetPostByIdRequest
import com.cainsgl.grpc.article.GetPostByIdResponse
import com.cainsgl.grpc.article.PostServiceGrpc
import io.grpc.stub.StreamObserver
import jakarta.annotation.Resource
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class PostServiceGrpcImpl : PostServiceGrpc.PostServiceImplBase()
{
    @Resource
    lateinit var postService: PostServiceImpl

    override fun getById(request: GetPostByIdRequest, responseObserver: StreamObserver<GetPostByIdResponse>)
    {
        val post = postService.getById(request.id)
        val response = if (post != null) {
            GetPostByIdResponse.newBuilder()
                .setExists(true)
                .setPost(post.toProto())
                .build()
        } else {
            GetPostByIdResponse.newBuilder()
                .setExists(false)
                .build()
        }
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    private fun PostEntity.toProto(): com.cainsgl.grpc.article.PostEntity
    {
        return com.cainsgl.grpc.article.PostEntity.newBuilder()
            .setId(this.id ?: 0)
            .setTitle(this.title)
            .setContent(this.content)
            .setSummary(this.summary)
            .setStatus(this.status.toProtoStatus())
            .setTop(this.top ?: false)
            .setRecommend(this.recommend ?: false)
            .setViewCount(this.viewCount ?: 0)
            .setLikeCount(this.likeCount ?: 0)
            .setCommentCount(this.commentCount ?: 0)
            .addAllTags(this.tags)
            .setUserId(this.userId ?: 0)
            .setCategoryId(this.categoryId ?: 0)
            .setSeoKeywords(this.seoKeywords)
            .setSeoDescription(this.seoDescription)
            .setCreatedAt(this.createdAt?.toString())
            .setUpdatedAt(this.updatedAt?.toString())
            .setPublishedAt(this.publishedAt?.toString())
            .setKbId(this.kbId ?: 0)
            .build()
    }

    private fun ArticleStatus?.toProtoStatus(): com.cainsgl.grpc.article.ArticleStatus
    {
        return when (this) {
            ArticleStatus.DRAFT -> com.cainsgl.grpc.article.ArticleStatus.ARTICLE_STATUS_DRAFT
            ArticleStatus.PENDING_REVIEW -> com.cainsgl.grpc.article.ArticleStatus.ARTICLE_STATUS_PENDING_REVIEW
            ArticleStatus.PUBLISHED -> com.cainsgl.grpc.article.ArticleStatus.ARTICLE_STATUS_PUBLISHED
            ArticleStatus.OFF_SHELF -> com.cainsgl.grpc.article.ArticleStatus.ARTICLE_STATUS_OFF_SHELF
            else -> com.cainsgl.grpc.article.ArticleStatus.ARTICLE_STATUS_UNSPECIFIED
        }
    }
}
