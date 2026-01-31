package com.cainsgl.article.controller

import cn.dev33.satoken.annotation.SaIgnore
import com.cainsgl.article.service.CategoryServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/post/category")
class CategoryController {

    @Resource
    lateinit var categoryService: CategoryServiceImpl

    /**
     * 根据ID获取分类
     */
    @GetMapping
    fun get(@RequestParam id:Long): Any {
        val category = categoryService.getCategory(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        return category
    }

}
