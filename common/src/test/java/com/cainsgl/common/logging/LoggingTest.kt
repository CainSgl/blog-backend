package com.cainsgl.common.logging

import org.junit.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 * 日志功能测试
 * 用于验证日志配置是否正确
 */
class LoggingTest {

    private val log = LoggerFactory.getLogger(LoggingTest::java.class)

    @Test
    fun testBasicLogging() {
        log.info("测试基本日志输出")
        log.warn("测试警告日志")
        log.error("测试错误日志")
        log.debug("测试调试日志")
    }

    @Test
    fun testStructuredLogging() {
        val userId = "12345"
        val orderId = "ORDER-001"
        val amount = 99.99

        log.info("测试结构化日志, userId={}, orderId={}, amount={}", userId, orderId, amount)
    }

    @Test
    fun testMdcLogging() {
        try {
            MDC.put("traceId", "test-trace-id-123")
            MDC.put("userId", "test-user-456")
            MDC.put("orderId", "test-order-789")

            log.info("测试 MDC 上下文日志")
            log.info("这条日志应该包含 traceId, userId, orderId")

        } finally {
            MDC.clear()
        }
    }

    @Test
    fun testExceptionLogging() {
        val exception = RuntimeException("测试异常")
        log.error("测试异常日志, orderId={}", "ORDER-001", exception)
    }

    @Test
    fun testJsonOutput() {
        // 这个测试用于验证 JSON 格式输出
        // 运行时应该看到 JSON 格式的日志（如果 LOG_FORMAT 未设置或设置为 JsonConsole）
        
        MDC.put("traceId", "json-test-trace-id")
        MDC.put("userId", "json-test-user")
        MDC.put("requestPath", "/api/test")
        MDC.put("requestMethod", "GET")
        MDC.put("clientIp", "192.168.1.100")

        log.info("JSON 格式测试日志")
        log.info("包含多个字段, field1={}, field2={}, field3={}", "value1", "value2", "value3")

        MDC.clear()
    }
}
