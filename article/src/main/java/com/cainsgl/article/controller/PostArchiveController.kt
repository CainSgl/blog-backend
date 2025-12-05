package com.cainsgl.article.controller

import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.article.service.PostArchiveServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/post-archive")
class PostArchiveController {

    @Resource
    lateinit var postArchiveService: PostArchiveServiceImpl

    /**
     * 根据ID获取归档记录
     */
    @GetMapping
    fun get(@RequestParam(required = false) id: Long?): Any {
        if (id == null) {
            return ResultCode.MISSING_PARAM
        }
        val archive = postArchiveService.getPostArchive(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        return archive
    }

}
