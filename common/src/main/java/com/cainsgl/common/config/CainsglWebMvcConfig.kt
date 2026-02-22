package com.cainsgl.common.config

import com.cainsgl.common.config.interceptor.LoggingMdcInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CainsglWebMvcConfig(
    private val loggingMdcInterceptor: LoggingMdcInterceptor
) : WebMvcConfigurer
{
    override fun addCorsMappings(registry: CorsRegistry)
    {
        registry.addMapping("/**")
            .allowedOriginPatterns(
                "https://cainsgl.top",
                "http://localhost:*",
                "http://127.0.0.1:*"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }

    override fun addInterceptors(registry: InterceptorRegistry)
    {
        // 注册日志 MDC 拦截器，为所有请求添加日志上下文
        registry.addInterceptor(loggingMdcInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/actuator/**",  // 排除健康检查端点
                "/error"         // 排除错误页面
            )
    }
}