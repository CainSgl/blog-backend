package com.cainsgl.ai.controller

import cn.dev33.satoken.annotation.SaCheckRole
import com.cainsgl.ai.service.AiServiceImpl
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ai")
class AiController
{
    @Resource
    lateinit var AiService: AiServiceImpl
    @SaCheckRole("admin")
    @PostMapping
    fun embedding():Any
    {
        return AiService.getEmbedding("我是学生")
    }
}