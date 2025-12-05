package com.cainsgl.article.controller

import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.article.service.PostChunkVectorServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/post-chunk-vector")
class PostChunkVectorController {

    @Resource
    lateinit var postChunkVectorService: PostChunkVectorServiceImpl

    /**
     * 根据ID获取向量记录
     */
    @GetMapping
    fun get(@RequestParam(required = false) id: Long?): Any {
        if (id == null) {
            return ResultCode.MISSING_PARAM
        }
        val vector = postChunkVectorService.getPostChunkVector(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        return vector
    }

}
