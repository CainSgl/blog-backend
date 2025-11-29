package com.cainsgl.common.config;

import com.cainsgl.common.dto.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalResponseBodyAdvice implements ResponseBodyAdvice<Object>
{
    private static final Set<String> EXCLUDE_PATHS = Set.of(
            "/actuator/health",
            "/actuator/metrics",
            "/actuator/info",
            "/actuator/prometheus",
            "/favicon.ico",
            "/swagger-ui/",
            "/v3/api-docs"
    );
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType)
    {
        return !returnType.getParameterType().equals(Result.class)
                && !returnType.getParameterType().equals(Void.TYPE);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response)
    {
        String requestPath = request.getURI().getPath();
        // 排除特定路径（健康检查等）
        if (EXCLUDE_PATHS.stream().anyMatch(requestPath::startsWith)) {
            return body;
        }
        // null值直接返回Result.success()
        if (body == null) {
            return Result.success();
        }
        // 已经是Result类型的不重复包装
        if (body instanceof Result) {
            return body;
        }
        // 普通对象包装为Result.success()
        return Result.success(body);
    }
}
