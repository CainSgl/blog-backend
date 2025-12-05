package com.cainsgl.article.controller

import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.article.service.CategoryServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/category")
class CategoryController {

    @Resource
    lateinit var categoryService: CategoryServiceImpl

    /**
     * 根据ID获取分类
     */
    @GetMapping
    fun get(@RequestParam(required = false) id: Long?): Any {
        if (id == null) {
            return ResultCode.MISSING_PARAM
        }
        val category = categoryService.getCategory(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        return category
    }

}
