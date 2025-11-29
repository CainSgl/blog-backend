package com.cainsgl.common.config;

import io.grpc.*;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

import java.util.UUID;

@GrpcGlobalServerInterceptor
@Component
public class TraceIdInterceptor implements ServerInterceptor
{

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next)
    {
        // 在 gRPC 入口统一设置 traceId 到 MDC
        // 优先从 OpenTelemetry 当前 Span 获取（Istio Ambient + OTel 自动接入时可直接拿到链路ID）
        String traceId = null;
        try
        {
            Span currentSpan = Span.current();
            if (currentSpan != null)
            {
                SpanContext spanContext = currentSpan.getSpanContext();
                if (spanContext != null && spanContext.isValid())
                {
                    traceId = spanContext.getTraceId();
                }
            }
        } catch (Exception ignored)
        {
            System.err.println("OpenTelemetry 获取TraceId失败");
        }

        // 若当前没有有效 Span（可能未启用 OTel 或非Istio环境），兜底生成一个内部 traceId
        if (traceId == null || traceId.isEmpty())
        {
            traceId = "noOtel-or-noIstio-" + UUID.randomUUID();
        }
        //这里是为了解耦日志，现在就不需要手动的输入trace了
        MDC.put("traceId", traceId);

        return Contexts.interceptCall(Context.current(), call, headers, next);
    }
}
