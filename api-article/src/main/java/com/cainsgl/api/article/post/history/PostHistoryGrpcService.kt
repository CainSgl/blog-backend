package com.cainsgl.api.article.post.history

import com.cainsgl.common.entity.article.PostHistoryEntity
import com.cainsgl.grpc.article.GetPostHistoryByIdRequest
import com.cainsgl.grpc.article.PostHistoryServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.PostHistoryServiceImpl"])
class PostHistoryGrpcService : PostHistoryService
{
    @GrpcClient("PostHistoryService")
    lateinit var postHistoryServiceGrpc: PostHistoryServiceGrpc.PostHistoryServiceBlockingStub

    override fun getLastById(postId: Long): PostHistoryEntity?
    {
        val request = GetPostHistoryByIdRequest.newBuilder()
            .setPostId(postId)
            .build()
        val response = postHistoryServiceGrpc.getLastById(request)
        if (!response.exists) return null
        return response.postHistory.toEntity()
    }

    private fun com.cainsgl.grpc.article.PostHistoryEntity.toEntity(): PostHistoryEntity
    {
        return PostHistoryEntity(
            id = this.id,
            userId = this.userId,
            postId = this.postId,
            content = this.content,
            createdAt = this.createdAt.takeIf { it.isNotEmpty() }?.let { 
                // 尝试解析为OffsetDateTime，然后转换为LocalDateTime
                try {
                    val offsetDateTime = OffsetDateTime.parse(it)
                    offsetDateTime.toLocalDateTime()
                } catch (e: Exception) {
                    // 如果解析失败，尝试直接解析为LocalDateTime
                    LocalDateTime.parse(it)
                }
            },
            version = this.version
        )
    }
}