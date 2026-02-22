package com.cainsgl.common.config.interceptor

import cn.dev33.satoken.stp.StpUtil
import io.opentelemetry.api.trace.Span
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.util.*

/**
 * 日志 MDC 拦截器
 * 为每个 HTTP 请求自动添加日志上下文信息（traceId, userId, requestPath 等）
 * 供 Fluent Bit 收集并发送到 Elasticsearch
 */
@Component
class LoggingMdcInterceptor : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        // 1. 设置 traceId（优先从 OpenTelemetry Span 获取）
        var traceId: String? = null
        try {
            val currentSpan = Span.current()
            if (currentSpan.spanContext.isValid) {
                traceId = currentSpan.spanContext.traceId
                MDC.put("spanId", currentSpan.spanContext.spanId)
            }
        } catch (ignored: Exception) {
            // OpenTelemetry 未启用或获取失败
        }

        // 兜底：生成内部 traceId
        if (traceId.isNullOrEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "")
        }
        MDC.put("traceId", traceId)

        // 2. 设置 userId（从 Sa-Token 获取）
        try {
            if (StpUtil.isLogin()) {
                MDC.put("userId", StpUtil.getLoginIdAsString())
            }
        } catch (ignored: Exception) {
            // 未登录或获取失败
        }

        // 3. 设置请求路径和方法
        MDC.put("requestPath", request.requestURI)
        MDC.put("requestMethod", request.method)

        // 4. 设置客户端 IP
        val clientIp = getClientIp(request)
        if (clientIp.isNotEmpty()) {
            MDC.put("clientIp", clientIp)
        }

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        // 请求结束后清理 MDC，避免内存泄漏
        MDC.clear()
    }

    /**
     * 获取客户端真实 IP
     * 考虑代理和负载均衡的情况
     */
    private fun getClientIp(request: HttpServletRequest): String {
        val headers = listOf(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        )

        for (header in headers) {
            val ip = request.getHeader(header)
            if (!ip.isNullOrEmpty() && !"unknown".equals(ip, ignoreCase = true)) {
                // X-Forwarded-For 可能包含多个 IP，取第一个
                return ip.split(",")[0].trim()
            }
        }

        return request.remoteAddr ?: ""
    }
}
