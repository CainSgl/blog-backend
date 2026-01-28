package com.cainsgl.user.util

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class HttpClientConfig
{
    @Bean
    fun restClient(): RestClient {
        return RestClient.builder()
            .requestFactory(org.springframework.http.client.JdkClientHttpRequestFactory())
            .build()
    }
}