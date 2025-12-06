package com.cainsgl.common.service.article.directory

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.article.service.DirectoryServiceImpl"])
class DirectoryGrpcService : DirectoryService
{
//    @GrpcClient("DirectoryService")
//    lateinit var directoryServiceGrpc: DirectoryServiceGrpc.DirectoryServiceBlockingStub

}
