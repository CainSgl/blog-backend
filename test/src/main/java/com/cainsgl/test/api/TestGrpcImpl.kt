package com.cainsgl.test.api

import com.cainsgl.common.service.test.TestService
import com.cainsgl.grpc.api.TestRequest
import com.cainsgl.grpc.api.TestResponse
import com.cainsgl.grpc.api.TestServiceGrpc
import io.grpc.stub.StreamObserver
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import net.devh.boot.grpc.server.service.GrpcService

//这是grpc的具体实现类，跟proto文件一一对应，正常来说远程调用，但是如果本地有本地实现，则会优先调用本地实现
@GrpcService
class TestGrpcImpl : TestServiceGrpc.TestServiceImplBase()
{
    private val log = LoggerFactory.getLogger(TestGrpcImpl::class.java)

    //这个是本地实现的Service
    @Resource
    private lateinit var testService: TestService

    override fun test(request: TestRequest, responseObserver: StreamObserver<TestResponse>)
    {
        val test = request.test
        val res = testService.sayHello(test)
        responseObserver.onNext(TestResponse.newBuilder().setMessage(res).build())
        log.info("接收到远程调用，参数为：{}", test)
        responseObserver.onCompleted()
    }
}
