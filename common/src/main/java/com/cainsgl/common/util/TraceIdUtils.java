package com.cainsgl.common.util;

import org.slf4j.MDC;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;


public class TraceIdUtils
{
    public static String getTraceId()
    {
        // 优先从MDC读取（入口Filter/Interceptor已经统一写入，解耦具体传输协议）
        String mdcTrace = MDC.get("traceId");
        if (mdcTrace != null && !mdcTrace.isEmpty())
        {
            return mdcTrace;
        }
        // 回退：OpenTelemetry 当前Span（在Istio Ambient + OTel环境可从Span获取全局traceId）
        try
        {
            Span currentSpan = Span.current();
            if (currentSpan != null)
            {
                SpanContext spanContext = currentSpan.getSpanContext();
                if (spanContext != null && spanContext.isValid())
                {
                    return spanContext.getTraceId();
                }
            }
        } catch (Exception ignored)
        {
        }
        // 兜底标记未知，以便上游按需处理
        return "unknown";
    }
}
