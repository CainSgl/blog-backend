package com.cainsgl.common.aspect

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

val log= KotlinLogging.logger {  }
@Aspect
@Component
class ApiPerformanceAspect {



    @Value("\${api.performance.threshold:3000}")
    private var threshold: Long = 3000


    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) || " +
            "within(@org.springframework.stereotype.Controller *)")
    fun controllerMethods() {}

    @Around("controllerMethods()")
    fun monitorApiPerformance(joinPoint: ProceedingJoinPoint): Any? {
        val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val request = requestAttributes?.request
        if (request == null || !request.requestURI.startsWith("/api")) {
            return joinPoint.proceed()
        }
        val startTime = System.currentTimeMillis()
        var result: Any? = null
        var exception: Throwable? = null

        try {
            result = joinPoint.proceed()
            return result
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            val executionTime = System.currentTimeMillis() - startTime

            // 如果执行时间超过阈值，记录错误日志
            if (executionTime > threshold) {
                logSlowApi(request, executionTime, joinPoint, exception)
            }
        }
    }


    private fun logSlowApi(
        request: HttpServletRequest,
        executionTime: Long,
        joinPoint: ProceedingJoinPoint,
        exception: Throwable?
    ) {
        val method = request.method
        val url = request.requestURI
        val queryString = request.queryString
        val fullUrl = if (queryString != null) "$url?$queryString" else url
        val clientIp = getClientIp(request)
        val className = joinPoint.signature.declaringTypeName
        val methodName = joinPoint.signature.name
        val status = if (exception != null) "FAILED" else "SUCCESS"

        log.error {
            "${"慢接口告警 | URL: {} | Method: {} | IP: {} | ExecutionTime: {}ms | Threshold: {}ms | Status: {} | Handler: {}.{}()"} ${
                arrayOf<Any?>(
                    fullUrl, method, clientIp, executionTime, threshold, status, className, methodName
                )
            }"
        }

        if (exception != null) {
            log.error { "${"接口执行异常: {}"} ${exception.message} $exception" }
        }
    }


    private fun getClientIp(request: HttpServletRequest): String {
        var ip = request.getHeader("X-Forwarded-For")
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("X-Real-IP")
        }
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }
        // 对于多级代理，取第一个IP
        if (!ip.isNullOrBlank() && ip.contains(",")) {
            ip = ip.split(",")[0].trim()
        }
        return ip ?: "unknown"
    }
}
