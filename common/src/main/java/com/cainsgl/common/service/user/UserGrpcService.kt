package com.cainsgl.common.service.user

import com.cainsgl.common.entity.user.UserEntity
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(type = ["com.cainsgl.user.service.UserServiceImpl"])
class UserGrpcService : UserService {
//    @GrpcClient("UserService")
//    lateinit var testServiceGrpc: TestServiceGrpc.TestServiceBlockingStub

    override fun getUser(id: Long): UserEntity? {
        return null
    }

    override fun updateById(userEntity: UserEntity): Boolean {
        return false
    }

    fun getUserByAccount(account: String): UserEntity? {
        // TODO: 实现gRPC调用
        return null
    }

    fun getExtra(id: Long): Map<*, *>? {
        return null
    }
}
