package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.user.service.UserNoticeServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/user/notice")
class UserNoticeController {

    @Resource
    lateinit var userNoticeService: UserNoticeServiceImpl

    @GetMapping
    fun getUserNotice(
        @RequestParam(required = false) type: String,
        @RequestParam(required = false) after: Long?,
        @RequestParam(defaultValue = "20") size: Int
    ): Map<String, Any?> {
        val userId = StpUtil.getLoginIdAsLong()
        return userNoticeService.getUserNoticeAndMarkChecked(userId, type, after, size)
    }


}
