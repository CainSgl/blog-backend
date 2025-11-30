package com.cainsgl.common.service.test

//注意，对应的service应该在对应的模块实现，不能在其他地方实现，其他地方的所有service都是对应的grpc代理
interface TestService {
    fun sayHello(who: String): String
}
