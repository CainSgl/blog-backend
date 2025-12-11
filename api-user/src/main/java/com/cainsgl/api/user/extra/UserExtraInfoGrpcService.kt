package com.cainsgl.api.user.extra

import com.cainsgl.grpc.user.GetInterestVectorRequest
import com.cainsgl.grpc.user.UserExtraInfoServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.user.service.UserExtraInfoServiceImpl"])
class UserExtraInfoGrpcService : UserExtraInfoService
{
    @GrpcClient("UserExtraInfoService")
    lateinit var userExtraInfoServiceGrpc: UserExtraInfoServiceGrpc.UserExtraInfoServiceBlockingStub

    override fun getInterestVector(userId: Long): FloatArray?
    {
        val request = GetInterestVectorRequest.newBuilder()
            .setUserId(userId)
            .build()
        val response = userExtraInfoServiceGrpc.getInterestVector(request)
        return if (response.interestVectorList.isEmpty()) null else response.interestVectorList.toFloatArray()
    }
}
