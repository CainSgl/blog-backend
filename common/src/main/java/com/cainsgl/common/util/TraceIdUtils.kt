package com.cainsgl.common.util

import org.slf4j.MDC
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext

object TraceIdUtils
{
    fun getTraceId(): String
    {
        // 优先从MDC读取（入口Filter/Interceptor已经统一写入，解耦具体传输协议）
        val mdcTrace = MDC.get("traceId")
        if (mdcTrace != null && mdcTrace.isNotEmpty())
        {
            return mdcTrace
        }
        // 回退：OpenTelemetry 当前Span（在Istio Ambient + OTel环境可从Span获取全局traceId）
        try
        {
            val currentSpan = Span.current()
            if (currentSpan != null)
            {
                val spanContext: SpanContext = currentSpan.spanContext
                if (spanContext.isValid)
                {
                    return spanContext.traceId
                }
            }
        } catch (ignored: Exception)
        {
        }
        // 兜底标记未知，以便上游按需处理
        return "unknown"
    }
}
