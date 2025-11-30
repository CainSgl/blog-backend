package com.cainsgl.aggregate.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix("/api") { _ -> true }
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        // 配置允许跨域的路径（/** 表示所有路径）
        registry.addMapping("/**")
            // 允许的源（* 表示所有域名，生产环境建议指定具体域名，如https://xxx.com）
            .allowedOriginPatterns("*") // 3.x 推荐用 allowedOriginPatterns 替代 allowedOrigins（避免通配符*的限制）
            // 允许的请求方法（GET/POST/PUT/DELETE/OPTIONS 等）
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            // 允许的请求头（* 表示所有）
            .allowedHeaders("*")
            // 是否允许携带 Cookie（跨域请求默认不携带，若需要则设为 true，且 allowedOriginPatterns 不能为 *，需指定具体域名）
            .allowCredentials(true)
            // 预检请求（OPTIONS）的缓存时间（秒），避免频繁发送预检请求
            .maxAge(3600)
    }
}
