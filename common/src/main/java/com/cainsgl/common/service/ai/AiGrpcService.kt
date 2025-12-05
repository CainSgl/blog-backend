package com.cainsgl.common.service.ai

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.ai.service.AiServiceImpl"])
class AiGrpcService : AiService {
//    @GrpcClient("AiService")
//    lateinit var aiServiceGrpc: AiServiceGrpc.AiServiceBlockingStub

}
