package com.cainsgl.article.controller


import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.apache.rocketmq.client.apis.producer.SendReceipt
import org.apache.rocketmq.client.core.RocketMQClientTemplate
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture


private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/post")
class PostController
{

    @Resource
    lateinit var postService: PostServiceImpl

    @Resource
    private lateinit var rocketMQClientTemplate: RocketMQClientTemplate

    /**
     * 根据ID获取文章
     */
    @GetMapping
    fun get(@RequestParam(required = false) id: Long?): Any
    {
        if (id == null)
        {
            return ResultCode.MISSING_PARAM
        }
//        val a = CompletableFuture<SendReceipt>()
//        val b = { a: String -> println(a) }
//        val c=CompletableFuture<SendReceipt>()
//        c.whenComplete { result, e ->
//                log.info { "result: $result" }
//                println("test")
//        }
//        rocketMQClientTemplate.asyncSendNormalMessage("a:1", "test", c)

        val post = postService.getPost(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        return post
    }

}
