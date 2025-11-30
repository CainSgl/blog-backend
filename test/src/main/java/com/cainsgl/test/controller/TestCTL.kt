package com.cainsgl.test.controller

import co.elastic.clients.elasticsearch.ElasticsearchClient
import com.cainsgl.common.service.test.TestService
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/test")
class TestCTL
{
    private val log = LoggerFactory.getLogger(TestCTL::class.java)

    @Resource
    private lateinit var elasticsearchClient: ElasticsearchClient

    @Resource
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Resource
    private lateinit var testService: TestService

    @GetMapping("/es")
    fun testEsIndexExists(): Map<String, Any>
    {
        val result = HashMap<String, Any>()
        try
        {
            // 检查索引是否存在
            val isExists = elasticsearchClient.indices()
                .exists { b -> b.index("test-index") }
                .value()
            // 封装返回结果
            result["success"] = true
            result["exists"] = isExists
            result["msg"] = "ElasticsearchClient 调用成功"
        } catch (e: Exception)
        {
            // 捕获异常，方便排查问题
            result["success"] = false
            result["msg"] = "ElasticsearchClient 调用失败：" + e.message
            result["errorDetail"] = e.cause?.message ?: "无详细原因"
        }
        return result
    }

    @GetMapping("/redis")
    fun testRedisTTL(): Map<String, Any>
    {
        val result = HashMap<String, Any>()
        try
        {
            redisTemplate.opsForValue().set("testKey", "testValue", 30, TimeUnit.SECONDS)
            result["success"] = true
        } catch (e: Exception)
        {
            result["success"] = false
            result["msg"] = "ElasticsearchClient 调用失败：" + e.message
            result["errorDetail"] = e.cause?.message ?: "无详细原因"
        }
        // 仅返回最简单的提示
        return result
    }

    @GetMapping("/logstash")
    fun testlogstash(@RequestParam(required = false) message: String?): Any
    {
        log.error("测试日志: {}", message ?: "此信息请忽略，仅用于测试logstash接受日志能力")
        return "success"
    }

    @GetMapping("/service")
    fun testService(): Any
    {
        return testService.javaClass.name
    }
}
