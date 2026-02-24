package com.cainsgl.article.api

import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.grpc.article.*
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
        val response = if (post != null)
        {
            GetPostByIdResponse.newBuilder()
                .setExists(true)
                .setPost(post.toProto())
                .build()
        } else
        {
            GetPostByIdResponse.newBuilder()
                .setExists(false)
                .build()
        }
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getByIds(request: GetPostsByIdsRequest, responseObserver: StreamObserver<GetPostsByIdsResponse>)
    {
        val posts = postService.getByIds(request.idsList)
        val response = GetPostsByIdsResponse.newBuilder()
            .addAllPosts(posts.map { it.toProto() })
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getVectorById(request: GetVectorByIdRequest, responseObserver: StreamObserver<GetVectorByIdResponse>)
    {
        val vector: FloatArray? = postService.getVectorById(request.id)
        val floatList = vector?.toList() ?: emptyList()
        val response = GetVectorByIdResponse.newBuilder()
            .addAllFloatArray(floatList)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun addViewCount(request: AddViewCountRequest, responseObserver: StreamObserver<RemoveVectorResponse>)
    {
        //实现一下
        val success = postService.addViewCount(request.id, request.count)
        val response = RemoveVectorResponse.newBuilder().setSuccess(success).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun addCommentCount(request: AddViewCountRequest, responseObserver: StreamObserver<RemoveVectorResponse>)
    {
        val success = postService.addCommentCount(request.id, request.count)
        val response = RemoveVectorResponse.newBuilder().setSuccess(success).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun saveToElasticsearch(request: SaveToElasticsearchRequest, responseObserver: StreamObserver<RemoveVectorResponse>)
    {
        val success = postService.saveToElasticsearch(
            postId = request.postId,
            title = request.title,
            summary = if (request.hasSummary()) request.summary else null,
            img = if (request.hasImg()) request.img else null,
            content = request.content,
            tags = request.tagsList
        )
        val response = RemoveVectorResponse.newBuilder().setSuccess(success).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun removeCache(request: RemoveCacheRequest, responseObserver: StreamObserver<RemoveVectorResponse>)
    {
        postService.removeCache(request.postId)
        val response = RemoveVectorResponse.newBuilder().setSuccess(true).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    private fun PostEntity.toProto(): com.cainsgl.grpc.article.PostEntity
    {
        return com.cainsgl.grpc.article.PostEntity.newBuilder()
            .setId(this.id ?: 0)
            .setTitle(this.title ?: "")
            .setContent(this.content ?: "")
            .setSummary(this.summary ?: "")
            .setStatus(this.status.toProtoStatus())
            .setTop(this.top ?: false)
            .setRecommend(this.recommend ?: false)
            .setViewCount(this.viewCount ?: 0)
            .setLikeCount(this.likeCount ?: 0)
            .setCommentCount(this.commentCount ?: 0)
            .addAllTags(this.tags ?: emptyList())
            .setUserId(this.userId ?: 0)
            .setCategoryId(this.categoryId ?: 0)
            .setSeoKeywords(this.seoKeywords ?: "")
            .setSeoDescription(this.seoDescription ?: "")
            .setCreatedAt(this.createdAt?.toString() ?: "")
            .setUpdatedAt(this.updatedAt?.toString() ?: "")
            .setPublishedAt(this.publishedAt?.toString() ?: "")
            .setKbId(this.kbId ?: 0)
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
