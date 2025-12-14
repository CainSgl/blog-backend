package com.cainsgl.user.api

import com.cainsgl.grpc.user.ProcessLogRequest
import com.cainsgl.grpc.user.ProcessLogResponse
import com.cainsgl.grpc.user.UserLogServiceGrpc
import com.cainsgl.user.service.UserLogServiceImpl
import io.grpc.stub.StreamObserver
import jakarta.annotation.Resource
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class UserLogServiceGrpcImpl : UserLogServiceGrpc.UserLogServiceImplBase()
{
    @Resource
    lateinit var userLogService: UserLogServiceImpl

    override fun loadLogsToRedis(request: ProcessLogRequest, responseObserver: StreamObserver<ProcessLogResponse>)
    {
        val result = userLogService.loadLogsToRedis(request.value)
        val response = ProcessLogResponse.newBuilder()
            .setResult(result)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
