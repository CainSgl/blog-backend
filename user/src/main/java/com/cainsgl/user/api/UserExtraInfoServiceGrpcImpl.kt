package com.cainsgl.user.api

import com.cainsgl.grpc.user.*
import com.cainsgl.user.service.UserExtraInfoServiceImpl
import io.grpc.stub.StreamObserver
import jakarta.annotation.Resource
import net.devh.boot.grpc.server.service.GrpcService

@GrpcService
class UserExtraInfoServiceGrpcImpl : UserExtraInfoServiceGrpc.UserExtraInfoServiceImplBase()
{
    @Resource
    lateinit var userExtraInfoService: UserExtraInfoServiceImpl

    override fun getInterestVector(request: GetInterestVectorRequest, responseObserver: StreamObserver<GetInterestVectorResponse>)
    {
        val interestVector = userExtraInfoService.getInterestVector(request.userId)
        val response = GetInterestVectorResponse.newBuilder()
            .addAllInterestVector(interestVector?.toList() ?: emptyList())
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun setInterestVector(request: SetInterestVectorRequest, responseObserver: StreamObserver<SetInterestVectorResponse>)
    {
        val success =
            userExtraInfoService.setInterestVector(request.userId, request.interestVectorList.toFloatArray())
        val response = SetInterestVectorResponse.newBuilder()
            .setSuccess(success)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
