package com.cainsgl.api.user

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.user.service.UserServiceImpl"])
class UserGrpcService : UserService {
//    @GrpcClient("UserService")
//    lateinit var testServiceGrpc: TestServiceGrpc.TestServiceBlockingStub

}
