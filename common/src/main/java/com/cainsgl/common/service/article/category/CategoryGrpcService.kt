package com.cainsgl.common.service.article.category

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.CategoryServiceImpl"])
class CategoryGrpcService : CategoryService
{
//    @GrpcClient("CategoryService")
//    lateinit var categoryServiceGrpc: CategoryServiceGrpc.CategoryServiceBlockingStub

}
