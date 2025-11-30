package com.cainsgl.common.config

import com.alibaba.fastjson2.JSON
import com.cainsgl.common.dto.response.Result
import com.cainsgl.common.dto.response.ResultCode
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@RestControllerAdvice
class GlobalResponseBodyAdvice : ResponseBodyAdvice<Any>
{
    private val log = LoggerFactory.getLogger(GlobalResponseBodyAdvice::class.java)

    companion object
    {
        private val EXCLUDE_PATHS = setOf(
            "/actuator/health",
            "/actuator/metrics",
            "/actuator/info",
            "/actuator/prometheus",
            "/favicon.ico",
            "/swagger-ui/",
            "/v3/api-docs"
        )
    }

    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>
    ): Boolean
    {
        return returnType.parameterType != Result::class.java &&
                returnType.parameterType != Void.TYPE
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any?
    {
        val requestPath = request.uri.path
        // 排除特定路径（健康检查等）
        if (EXCLUDE_PATHS.any { requestPath.startsWith(it) })
        {
            return body
        }
        // null值直接返回Result.success()
        if (body == null)
        {
            return Result.success()
        }
        // 已经是Result类型的不重复包装
        if (body is Result)
        {
            return body
        }
        if(body is ResultCode)
        {
            return body.defaultResult
        }
        val result = Result.success(body)
        if (body is String)
        {
            return JSON.toJSONString(result)
        }
        return result
    }
}
