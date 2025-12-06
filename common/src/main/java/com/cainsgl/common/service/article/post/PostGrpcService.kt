package com.cainsgl.common.service.article.post

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.PostServiceImpl"])
class PostGrpcService : PostService
{
//    @GrpcClient("PostService")
//    lateinit var postServiceGrpc: PostServiceGrpc.PostServiceBlockingStub

}
