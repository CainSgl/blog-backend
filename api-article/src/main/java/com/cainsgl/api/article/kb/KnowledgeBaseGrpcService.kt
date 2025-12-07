package com.cainsgl.api.article.kb

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.KnowledgeBaseServiceImpl"])
class KnowledgeBaseGrpcService : KnowledgeBaseService
{
//    @GrpcClient("KnowledgeBaseService")
//    lateinit var knowledgeBaseServiceGrpc: KnowledgeBaseServiceGrpc.KnowledgeBaseServiceBlockingStub

}
