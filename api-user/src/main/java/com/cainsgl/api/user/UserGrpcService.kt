package com.cainsgl.api.user

import com.cainsgl.grpc.user.MallocMemoryRequest
import com.cainsgl.grpc.user.MallocMemoryResponse
import com.cainsgl.grpc.user.UserServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.user.service.UserServiceImpl"])
class UserGrpcService : UserService {
    @GrpcClient("UserService")
    lateinit var userServiceGrpc: UserServiceGrpc.UserServiceBlockingStub

    override fun mallocMemory(userId: Long, memory: Int): Boolean {
        val request = MallocMemoryRequest.newBuilder()
            .setUserId(userId)
            .setMemory(memory)
            .build()
        val response = userServiceGrpc.mallocMemory(request)
        return response.success
    }
}
