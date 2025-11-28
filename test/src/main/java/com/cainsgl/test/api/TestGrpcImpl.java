package com.cainsgl.test.api;

import com.cainsgl.common.service.TestService;
import com.cainsgl.grpc.api.TestRequest;
import com.cainsgl.grpc.api.TestResponse;
import com.cainsgl.grpc.api.TestServiceGrpc;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import net.devh.boot.grpc.server.service.GrpcService;

//这是grpc的具体实现类，跟proto文件一一对应，正常来说远程调用，但是如果本地有本地实现，则会优先调用本地实现
@GrpcService
public class TestGrpcImpl extends TestServiceGrpc.TestServiceImplBase
{
    //这个是本地实现的Service
    @Resource
    private TestService testService;

    @Override
    public void test(TestRequest request, StreamObserver<TestResponse> responseObserver)
    {
        String test = request.getTest();
        String res = testService.sayHello(test);
        responseObserver.onNext(TestResponse.newBuilder().setMessage(res).build());
        responseObserver.onCompleted();
    }
}
