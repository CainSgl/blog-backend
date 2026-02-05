package com.cainsgl.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CainsglWebMvcConfig : WebMvcConfigurer
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
}