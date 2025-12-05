package com.cainsgl.common.service.article

import com.cainsgl.common.entity.article.PostEntity
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.PostServiceImpl"])
class PostGrpcService : PostService {
//    @GrpcClient("PostService")
//    lateinit var postServiceGrpc: PostServiceGrpc.PostServiceBlockingStub

}
