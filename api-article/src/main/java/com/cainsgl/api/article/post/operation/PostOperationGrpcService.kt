package com.cainsgl.api.article.post.operation

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.PostOperationServiceImpl"])
class PostOperationGrpcService : PostOperationService {
//    @GrpcClient("PostOperationService")
//    lateinit var postOperationServiceGrpc: PostOperationServiceGrpc.PostOperationServiceBlockingStub

}