package com.cainsgl.common.config

import cn.dev33.satoken.stp.StpUtil
import io.grpc.*
import org.slf4j.LoggerFactory

/**
 * gRPC 客户端拦截器：全局添加 SaToken 到请求头
 */
@Deprecated("该类已经被废弃，微服务调用目前不需要token和用服检查，目前是最高信任")
//@Component
class SaTokenGrpcClientInterceptor : ClientInterceptor
{
    private val log = LoggerFactory.getLogger(SaTokenGrpcClientInterceptor::class.java)

    companion object
    {
        private val SA_TOKEN_HEADER = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER)
    }

    override fun <ReqT, RespT> interceptCall(method: MethodDescriptor<ReqT, RespT>, callOptions: CallOptions, next: Channel): ClientCall<ReqT, RespT>
    {

        // 1. 获取当前上下文的 SaToken 令牌（未登录则为空）
        val token = StpUtil.getTokenValue()
        if (token == null || token.isEmpty())
        {
            log.warn("gRPC调用[{}]未获取到SaToken，可能未登录", method.fullMethodName)
            return next.newCall(method, callOptions)
        }

        // 2. 构建 Metadata 并注入 Token
        val metadata = Metadata()
        metadata.put(SA_TOKEN_HEADER, token)
        // 3. 传递 Metadata 并继续调用
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions))
        {
            override fun start(responseListener: Listener<RespT>, headers: Metadata)
            {
                // 将 Token 注入到请求头
                headers.merge(metadata)
                super.start(responseListener, headers)
                log.debug("gRPC调用[{}]已添加SaToken到请求头", method.fullMethodName)
            }
        }
    }
}
