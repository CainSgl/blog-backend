package com.cainsgl.user.controller

import cn.dev33.satoken.annotation.SaCheckRole
import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.user.dto.request.UserLogPostRequest
import com.cainsgl.user.service.UserLogServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/log")
class UserLogController
{

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Resource
    lateinit var userLogService: UserLogServiceImpl

    @SaCheckRole("admin")
    @GetMapping("/load")
    fun load(@RequestParam number:Int=10): Any
    {
        return userLogService.loadLogsToRedis(number)
    }

    @SaCheckRole("user")
    @PostMapping
    fun post(userLogPostRequest: UserLogPostRequest): Any
    {
        UserLogEntity.validAction(userLogPostRequest.action)
        TODO()
    }


}