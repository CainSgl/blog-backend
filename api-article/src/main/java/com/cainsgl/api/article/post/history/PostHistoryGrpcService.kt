package com.cainsgl.api.article.post.history

import com.cainsgl.common.entity.article.PostHistoryEntity
import com.cainsgl.grpc.article.GetPostHistoryByIdRequest
import com.cainsgl.grpc.article.PostHistoryServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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

    override fun updateById(historyId: Long, content: String): Boolean
    {
        val request = com.cainsgl.grpc.article.UpdateByIdRequest.newBuilder()
            .setHistoryId(historyId)
            .setContent(content)
            .build()
        val response = postHistoryServiceGrpc.updateById(request)
        return response.success
    }

    override fun createNewVersion(userId: Long, postId: Long, version: Int, content: String): Boolean
    {
        val request = com.cainsgl.grpc.article.CreateNewVersionRequest.newBuilder()
            .setUserId(userId)
            .setPostId(postId)
            .setVersion(version)
            .setContent(content)
            .build()
        val response = postHistoryServiceGrpc.createNewVersion(request)
        return response.success
    }

    private fun com.cainsgl.grpc.article.PostHistoryEntity.toEntity(): PostHistoryEntity
    {
        return PostHistoryEntity(
            id = this.id,
            userId = this.userId,
            postId = this.postId,
            content = this.content,
            createdAt = this.createdAt.takeIf { it.isNotEmpty() }?.let {
                try {
                    val dateTime = LocalDateTime.parse(it)
                   return@let dateTime
                } catch (e: Exception) {
                    // 如果解析失败，尝试直接解析为LocalDateTime
                    LocalDateTime.parse(it)
                }
            },
            version = this.version
        )
    }
}