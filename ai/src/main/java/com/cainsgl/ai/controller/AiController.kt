package com.cainsgl.ai.controller

import com.cainsgl.ai.service.AiServiceImpl
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ai")
class AiController
{
    @Resource
    lateinit var AiService: AiServiceImpl



}