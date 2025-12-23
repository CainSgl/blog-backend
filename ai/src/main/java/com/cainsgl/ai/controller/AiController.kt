package com.cainsgl.ai.controller

import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.ai.dto.request.ContentRequest
import com.cainsgl.ai.service.AiServiceImpl
import com.cainsgl.common.annotation.RateLimitByToken
import com.volcengine.ark.runtime.service.ArkService
import jakarta.annotation.Resource
import jakarta.validation.Valid
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter


@RestController
@RequestMapping("/ai")
class AiController
{
    @Resource
    lateinit var aiService: AiServiceImpl

    @Resource
    lateinit var arkService: ArkService

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @SaCheckRole("user")
    @PostMapping("/tag/generate")
    @RateLimitByToken(message = "生成标签接口不能频繁访问", interval = 10000, limit = 1)
    fun getTagsByAI(@RequestBody @Valid generateTagsRequest: ContentRequest): Any
    {
        return aiService.getTagsByContent(generateTagsRequest.content)
    }
    @SaCheckRole("user")
    @PostMapping("/summary/generate")
    @RateLimitByToken(message = "生成标签接口不能频繁访问", interval = 10000, limit = 1)
    fun getSummaryByAI(@RequestBody @Valid generateTagsRequest: ContentRequest): Any
    {
        return aiService.getSummaryByContent(generateTagsRequest.content)
    }

    @PostMapping("/chat", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @RateLimitByToken(message = "聊天过于频繁", interval = 10000, limit = 1)
    fun chat(@RequestBody @Valid contentRequest: ContentRequest): SseEmitter
    {
        //这个不能加载注解上，对于长聊天，有bug
        StpUtil.checkRole("user")
        val emitter = SseEmitter(60 * 1000L)
        aiService.chat(contentRequest.content, emitter, StpUtil.getLoginIdAsLong())
        return emitter
    }
    @SaCheckRole("user")
    @GetMapping("/history")
    fun chatHistory(): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        //获取历史对话信息，历史记录只保存7天
        var value = redisTemplate.opsForValue().get("${AiServiceImpl.AI_CHAT_NUM_REDIS_PREFIX}$userId")
        if(value == null)
        {
            //TODO 目前限制为10条
            value=10
        }
        val range = redisTemplate.opsForList().range("${AiServiceImpl.AI_CHAT_REDIS_PREFIX}$userId", 0, -1)
        return Pair(value.toString().toInt(),range)
    }

}