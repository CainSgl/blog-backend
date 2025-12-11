package com.cainsgl.common.config.misc

import org.springframework.boot.context.event.ApplicationPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.env.ConfigurableEnvironment

class EnvConfigPrinter : ApplicationListener<ApplicationPreparedEvent>
{
    override fun onApplicationEvent(event: ApplicationPreparedEvent)
    {
        val environment = event.applicationContext.environment as ConfigurableEnvironment
        printEnvConfig(environment)
    }

    private fun printEnvConfig(environment: ConfigurableEnvironment)
    {
        val configKeys = listOf(
            // 数据库配置
            "db.url", "db.username", "db.password", "db.pool.min-idle", "db.pool.max-size",
            // Redis配置
            "redis.host", "redis.port", "redis.password",
            // Elasticsearch配置
            "es.uris", "es.username", "es.password",
            // RocketMQ配置
            "rocketmq.endpoints", "rocketmq.ssl-enabled",
            // Logstash配置
            "logstash.host", "logstash.port",
            // 服务端口配置
            "server.http-port", "server.grpc-port",
            // 系统配置
            "system.cpu-cores", "system.shutdown-timeout", "system.grpc-await-termination", "system.profiles-active",
            // AI服务配置
            "ai.base-url", "ai.api-key", "ai.chat.model", "ai.chat.max-tokens",
            "ai.embedding.api-key", "ai.embedding.base-url", "ai.embedding.model",
            // gRPC服务地址配置
            "grpc-client.ai-service", "grpc-client.post-service", "grpc-client.user-service", "grpc-client.go-service"
        )

        val sb = StringBuilder("\n")
        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n")
        sb.append("║                        ENV CONFIGURATION LOADED                              ║\n")
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n")

        var currentSection = ""
        for (key in configKeys)
        {
            val section = key.substringBefore(".")
            if (section != currentSection)
            {
                currentSection = section
                sb.append("╟──────────────────────────────────────────────────────────────────────────────╢\n")
                sb.append("║ [$currentSection]\n")
            }
            val value = environment.getProperty(key) ?: "<未设置>"
            val maskedValue = maskSensitiveValue(key, value)
            sb.append("║   $key = $maskedValue\n")
        }

        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝")
        println(sb)
    }

    private fun maskSensitiveValue(key: String, value: String): String
    {
        val sensitiveKeys = listOf("password", "api-key", "api_key", "secret")

        if (sensitiveKeys.any { key.contains(it, ignoreCase = true) })
        {
            return if (value.length > 4)
            {
                "${value.take(2)}****${value.takeLast(2)}"
            } else
            {
                "****"
            }
        }
        return value
    }
}
