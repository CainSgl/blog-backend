package com.cainsgl.api.article.post.chunk

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.PostChunkVectorServiceImpl"])
class PostChunkVectorGrpcService : PostChunkVectorService
{
//    @GrpcClient("PostChunkVectorService")
//    lateinit var postChunkVectorServiceGrpc: PostChunkVectorServiceGrpc.PostChunkVectorServiceBlockingStub

}
