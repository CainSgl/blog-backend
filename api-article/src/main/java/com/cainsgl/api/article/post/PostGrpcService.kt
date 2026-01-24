package com.cainsgl.api.article.post

import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.grpc.article.AddViewCountRequest
import com.cainsgl.grpc.article.GetPostByIdRequest
import com.cainsgl.grpc.article.GetPostsByIdsRequest
import com.cainsgl.grpc.article.GetVectorByIdRequest
import com.cainsgl.grpc.article.PostServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.PostServiceImpl"])
class PostGrpcService : PostService
{
    @GrpcClient("PostService")
    lateinit var postServiceGrpc: PostServiceGrpc.PostServiceBlockingStub

    override fun getById(id: Long): PostEntity?
    {
        val request = GetPostByIdRequest.newBuilder()
            .setId(id)
            .build()
        val response = postServiceGrpc.getById(request)
        if (!response.exists) return null
        return response.post.toEntity()
    }

    override fun getByIds(ids: List<Long>): List<PostEntity>
    {
        val request = GetPostsByIdsRequest.newBuilder()
            .addAllIds(ids)
            .build()
        val response = postServiceGrpc.getByIds(request)
        return response.postsList.map { it.toEntity() }
    }

    override fun getVectorById(id: Long): FloatArray?
    {
        val request = GetVectorByIdRequest.newBuilder()
            .setId(id)
            .build()
        val response = postServiceGrpc.getVectorById(request)
        val floatArrayList = response.floatArrayList
        if (floatArrayList.isEmpty()) return null
        return floatArrayList.toFloatArray()
    }

    override fun addViewCount(id: Long, count: Int):Boolean
    {
        val request = AddViewCountRequest.newBuilder()
            .setId(id)
            .build()
        val response = postServiceGrpc.addViewCount(request)
        return response.success
    }

    override fun addCommentCount(id: Long, count: Int): Boolean
    {
        val request = AddViewCountRequest.newBuilder()
            .setId(id)
            .build()
        val response = postServiceGrpc.addCommentCount(request)
        return response.success
    }

    private fun com.cainsgl.grpc.article.PostEntity.toEntity(): PostEntity
    {
        return PostEntity(
            id = this.id,
            title = this.title,
            content = this.content,
            summary = this.summary,
            status = ArticleStatus.fromNumber(this.status.number),
            top = this.top,
            recommend = this.recommend,
            viewCount = this.viewCount,
            likeCount = this.likeCount,
            commentCount = this.commentCount,
            tags = this.tagsList,
            userId = this.userId,
            categoryId = this.categoryId,
            seoKeywords = this.seoKeywords,
            seoDescription = this.seoDescription,
            createdAt = this.createdAt.takeIf { it.isNotEmpty() }?.let { LocalDateTime.parse(it) },
            updatedAt = this.updatedAt.takeIf { it.isNotEmpty() }?.let { LocalDateTime.parse(it) },
            publishedAt = this.publishedAt.takeIf { it.isNotEmpty() }?.let { LocalDateTime.parse(it) },
            kbId = this.kbId
        )
    }
}
