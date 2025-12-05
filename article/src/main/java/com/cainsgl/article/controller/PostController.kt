package com.cainsgl.article.controller

import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.article.service.PostServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/post")
class PostController {

    @Resource
    lateinit var postService: PostServiceImpl

    /**
     * 根据ID获取文章
     */
    @GetMapping
    fun get(@RequestParam(required = false) id: Long?): Any {
        if (id == null) {
            return ResultCode.MISSING_PARAM
        }
        val post = postService.getPost(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        return post
    }

}
