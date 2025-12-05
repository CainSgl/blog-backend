package com.cainsgl.common.service.article

import com.cainsgl.common.entity.article.PostArchiveEntity
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.PostArchiveServiceImpl"])
class PostArchiveGrpcService : PostArchiveService {
//    @GrpcClient("PostArchiveService")
//    lateinit var postArchiveServiceGrpc: PostArchiveServiceGrpc.PostArchiveServiceBlockingStub

}
