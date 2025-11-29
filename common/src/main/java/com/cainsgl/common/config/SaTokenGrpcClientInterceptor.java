package com.cainsgl.common.config;

import cn.dev33.satoken.stp.StpUtil;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * gRPC 客户端拦截器：全局添加 SaToken 到请求头
 */
@Slf4j
@Component
public class SaTokenGrpcClientInterceptor implements ClientInterceptor
{
    private static final Metadata.Key<String> SA_TOKEN_HEADER = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next)
    {

        // 1. 获取当前上下文的 SaToken 令牌（未登录则为空）
        String token = StpUtil.getTokenValue();
        if (token == null || token.isEmpty())
        {
            log.warn("gRPC调用[{}]未获取到SaToken，可能未登录", method.getFullMethodName());
            return next.newCall(method, callOptions);
        }

        // 2. 构建 Metadata 并注入 Token
        Metadata metadata = new Metadata();
        metadata.put(SA_TOKEN_HEADER, token);
        // 3. 传递 Metadata 并继续调用
        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, callOptions))
        {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers)
            {
                // 将 Token 注入到请求头
                headers.merge(metadata);
                super.start(responseListener, headers);
                log.debug("gRPC调用[{}]已添加SaToken到请求头", method.getFullMethodName());
            }
        };
    }
}