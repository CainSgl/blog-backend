package com.cainsgl.ai.core

import com.cainsgl.common.exception.BSystemException
import com.volcengine.ark.runtime.service.ArkService
import okhttp3.Dispatcher
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors


@Configuration
class Ai(
    @Value("\${spring.ai.openai.api-key}")
    var apiKey: String
)
{
    @Bean
    fun arkService(): ArkService
    {
        if (apiKey.isEmpty())
        {
            throw BSystemException("API key is empty")
        }
        val dispatcher=Dispatcher(Executors.newVirtualThreadPerTaskExecutor())
        return ArkService.builder().apiKey(apiKey).dispatcher(dispatcher).baseUrl("https://ark.cn-beijing.volces.com/api/v3/chat/completions").build()
    }
}