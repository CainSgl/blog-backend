package com.cainsgl.common.service.test;

import com.cainsgl.grpc.api.TestRequest;
import com.cainsgl.grpc.api.TestResponse;
import com.cainsgl.grpc.api.TestServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;


//这个是备选实现，只要当不存在
@Service
@ConditionalOnMissingBean(type = "com.cainsgl.test.service.TestServiceImpl")
public class TestGrpcService implements TestService
{
    @GrpcClient("TestService")
    TestServiceGrpc.TestServiceBlockingStub testServiceGrpc;

    @Override
    public String sayHello(String who)
    {
        TestRequest build = TestRequest.newBuilder().setTest(who).build();
        TestResponse test = testServiceGrpc.test(build);
        return test.getMessage();
    }
}
