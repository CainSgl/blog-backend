package com.cainsgl.user.controller

import cn.dev33.satoken.annotation.SaCheckRole
import com.cainsgl.user.service.UserLogServiceImpl
import com.cainsgl.user.service.UserServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/log")
class UserLogController
{
    @Resource
    lateinit var userService: UserServiceImpl

    @Resource
    lateinit var userLogService: UserLogServiceImpl

    @SaCheckRole("admin")
    @GetMapping("/process")
    fun processLog(@RequestParam number:Int=10): Any
    {
       return userLogService.processLog(number)
    }
}