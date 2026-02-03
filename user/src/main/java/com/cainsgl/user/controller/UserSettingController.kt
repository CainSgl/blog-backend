package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.user.service.UserSettingServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/user/setting")
class UserSettingController
{
    @Resource
    lateinit var userSettingService: UserSettingServiceImpl

    /**
     * 获取当前用户设置
     */
    @GetMapping
    fun getSetting(): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val setting = userSettingService.getUserSetting(userId)
        return setting?.json ?: emptyMap<String, Any>()
    }

    /**
     * 保存或更新当前用户设置
     */
    @PutMapping
    fun saveSetting(@RequestBody json: Map<String, Any>): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        return if (userSettingService.saveOrUpdateSetting(userId, json))
        {
            ResultCode.SUCCESS
        } else
        {
            ResultCode.UNKNOWN_ERROR
        }
    }

    /**
     * 删除当前用户设置
     */
    @DeleteMapping
    fun deleteSetting(): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        return if (userSettingService.deleteSetting(userId))
        {
            ResultCode.SUCCESS
        } else
        {
            ResultCode.UNKNOWN_ERROR
        }
    }
}
