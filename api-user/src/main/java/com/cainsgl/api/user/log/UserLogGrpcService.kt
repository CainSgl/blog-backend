package com.cainsgl.api.user.log

import com.cainsgl.grpc.user.ProcessLogRequest
import com.cainsgl.grpc.user.UserLogServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.user.service.UserLogServiceImpl"])
class UserLogGrpcService : UserLogService
{
    @GrpcClient("UserLogService")
    lateinit var userLogServiceGrpc: UserLogServiceGrpc.UserLogServiceBlockingStub

    override fun processLog(value: Int): Int
    {
        val request = ProcessLogRequest.newBuilder()
            .setValue(value)
            .build()
        return userLogServiceGrpc.processLog(request).result
    }
}
