package com.cainsgl.api.article.post.chunk
import com.cainsgl.grpc.article.PostChunkVectorServiceGrpc
import com.cainsgl.grpc.article.ReloadVectorRequest
import com.cainsgl.grpc.article.RemoveVectorRequest
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.PostChunkVectorServiceImpl"])
class PostChunkVectorGrpcService : PostChunkVectorService
{
    @GrpcClient("PostChunkVectorService")
    lateinit var postChunkVectorServiceGrpc: PostChunkVectorServiceGrpc.PostChunkVectorServiceBlockingStub

    override fun reloadVector(postId: Long,originContent: String?): Boolean
    {
        val builder = ReloadVectorRequest.newBuilder()
            .setPostId(postId)
        originContent?.let { builder.setOriginContent(it) }
        val response = postChunkVectorServiceGrpc.reloadVector(builder.build())
        return response.success
    }

    override fun removeVector(postId: Long): Boolean
    {
        val request = RemoveVectorRequest.newBuilder()
            .setPostId(postId)
            .build()
        val response = postChunkVectorServiceGrpc.removeVector(request)
        return response.success
    }
}
