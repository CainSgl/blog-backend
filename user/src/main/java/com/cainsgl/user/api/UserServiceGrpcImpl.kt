package com.cainsgl.user.api

import com.cainsgl.grpc.user.*
import com.cainsgl.user.service.UserServiceImpl
import io.grpc.stub.StreamObserver
import jakarta.annotation.Resource
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class UserServiceGrpcImpl : UserServiceGrpc.UserServiceImplBase() {
    @Resource
    lateinit var userService: UserServiceImpl

    override fun mallocMemory(request: MallocMemoryRequest, responseObserver: StreamObserver<MallocMemoryResponse>) {
        val success = userService.mallocMemory(request.userId, request.memory)
        val response = MallocMemoryResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun createNotice(request: CreateNoticeRequest, responseObserver: StreamObserver<CreateNoticeResponse>) {
        val success = userService.createNotice(request.targetId, request.type, request.userId, request.targetUser)
        val response = CreateNoticeResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}