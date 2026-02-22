package com.cainsgl.common.util

import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 * 日志记录示例
 * 展示如何正确使用日志记录功能
 */
object LoggingExample {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 基本日志记录示例
     */
    fun basicLogging() {
        // 自动包含 traceId, userId, requestPath 等上下文信息
        log.info("用户操作成功")
        log.warn("缓存未命中，使用数据库查询")
        log.debug("查询参数: page=1, size=10")
    }

    /**
     * 结构化日志示例（推荐）
     */
    fun structuredLogging(userId: String, orderId: String, amount: Double) {
        // 使用占位符，避免字符串拼接
        log.info("订单创建成功, userId={}, orderId={}, amount={}", userId, orderId, amount)
    }

    /**
     * 异常日志示例
     */
    fun exceptionLogging(orderId: String, exception: Exception) {
        // 记录异常时包含上下文和堆栈信息
        log.error("订单处理失败, orderId={}", orderId, exception)
    }

    /**
     * 添加自定义 MDC 上下文示例
     */
    fun customMdcLogging(orderId: String, paymentMethod: String) {
        try {
            // 添加自定义上下文
            MDC.put("orderId", orderId)
            MDC.put("paymentMethod", paymentMethod)

            log.info("开始处理支付")
            // ... 业务逻辑 ...
            log.info("支付处理完成")

        } finally {
            // 清理自定义上下文（可选，请求结束时会自动清理）
            MDC.remove("orderId")
            MDC.remove("paymentMethod")
        }
    }

    /**
     * 条件日志示例（性能优化）
     */
    fun conditionalLogging(data: Any) {
        // 对于复杂的日志消息，使用条件判断避免不必要的计算
        if (log.isDebugEnabled) {
            log.debug("详细数据: {}", expensiveToString(data))
        }
    }

    private fun expensiveToString(data: Any): String {
        // 模拟耗时的字符串转换
        return data.toString()
    }

    /**
     * ❌ 错误示例 - 不要这样做
     */
    @Suppress("unused")
    fun badPractices(password: String, userId: String) {
        // ❌ 不要使用 System.out.println
        // System.out.println("用户登录")

        // ❌ 不要在日志中输出敏感信息
        // log.info("用户登录: password={}", password)

        // ❌ 不要使用字符串拼接
        // log.info("用户 " + userId + " 登录成功")

        // ✅ 正确做法
        log.info("用户登录成功, userId={}", userId)
    }
}
