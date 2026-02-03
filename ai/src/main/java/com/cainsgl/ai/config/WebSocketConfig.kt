package com.cainsgl.ai.config

import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.ai.websocket.ChatWebSocketHandler
import jakarta.annotation.Resource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.HandshakeInterceptor

@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {

    @Resource
    lateinit var chatWebSocketHandler: ChatWebSocketHandler

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
            .addInterceptors(AuthHandshakeInterceptor())
            //ONLY DEV
            .setAllowedOrigins("*")
    }

    @Bean
    fun servletServerContainerFactoryBean(): org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory {
        return org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory().apply {
            addServerCustomizers(org.springframework.boot.web.embedded.jetty.JettyServerCustomizer { server ->
                server.connectors.forEach { connector ->
                    if (connector is org.eclipse.jetty.server.ServerConnector) {
                        connector.idleTimeout = 300000 // 5分钟
                    }
                }
            })
        }
    }

    class AuthHandshakeInterceptor : HandshakeInterceptor {
        override fun beforeHandshake(
            request: ServerHttpRequest,
            response: ServerHttpResponse,
            wsHandler: WebSocketHandler,
            attributes: MutableMap<String, Any>
        ): Boolean {
            try {
                // 从请求参数中获取 token
                val query = request.uri.query ?: return false
                val token = query.split("&")
                    .find { it.startsWith("token=") }
                    ?.substringAfter("token=")
                    ?: return false
                
                // 验证 token 并获取用户 ID
                val loginId = StpUtil.getLoginIdByToken(token) ?: return false

                attributes["userId"] = loginId.toString().toLong()
                
                return true
            } catch (e: Exception) {
                return false
            }
        }

        override fun afterHandshake(
            request: ServerHttpRequest,
            response: ServerHttpResponse,
            wsHandler: WebSocketHandler,
            exception: Exception?
        ) {
        }
    }
}
