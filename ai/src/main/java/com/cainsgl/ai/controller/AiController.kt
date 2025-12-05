package com.cainsgl.ai.controller

import com.cainsgl.ai.service.AiServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.service.ai.AiService
import jakarta.annotation.Resource
import org.springframework.ai.embedding.EmbeddingModel

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/ai")
class AiController
{
    @Resource
    lateinit var AiService: AiServiceImpl



}