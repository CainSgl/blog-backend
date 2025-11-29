package com.cainsgl.common.service.user;

import com.cainsgl.common.entity.user.UserEntity;
import com.cainsgl.common.service.test.TestService;
import com.cainsgl.grpc.api.TestRequest;
import com.cainsgl.grpc.api.TestResponse;
import com.cainsgl.grpc.api.TestServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnMissingBean(type = "com.cainsgl.user.service.UserServiceImpl")
public class UserGrpcService implements UserService
{
//    @GrpcClient("UserService")
//    TestServiceGrpc.TestServiceBlockingStub testServiceGrpc;


    @Override
    public UserEntity getUser(long id)
    {
        return null;
    }

    @Override
    public boolean updateById(UserEntity userEntity)
    {
        return false;
    }

    @Override
    public UserEntity getUserByAccount(String account)
    {
        // TODO: 实现gRPC调用
        return null;
    }

    @Override
    public Map getExtra(long id)
    {
        return null;
    }
}

