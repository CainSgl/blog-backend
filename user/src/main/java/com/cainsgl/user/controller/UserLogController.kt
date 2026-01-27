package com.cainsgl.user.controller

import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.common.util.user.DeviceUtils
import com.cainsgl.user.dto.request.UserLogPostRequest
import com.cainsgl.user.service.UserLogServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/log")
class UserLogController
{

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Resource
    lateinit var userLogService: UserLogServiceImpl

    @SaCheckRole("admin")
    @GetMapping("/load")
    fun load(@RequestParam number:Int=10): Any
    {
        return userLogService.loadLogsToRedis(number)
    }

    @SaCheckRole("user")
    @PostMapping("/article")
    fun articlePost(userLogPostRequest: UserLogPostRequest): Any
    {
        if(!userLogPostRequest.action.startsWith("article."))
        {
            return ResultCode.PARAM_INVALID
        }
        if (UserLogEntity.validAction(userLogPostRequest.action))
        {
            return ResultCode.PARAM_INVALID
        }
        //article 的info是需要log.info!!["postId"]
        val map=mapOf("postId" to userLogPostRequest.id)
            //构建
            val userId=StpUtil.getLoginIdAsLong()
            val userLogEntity =
                UserLogEntity(userId = userId, action = userLogPostRequest.action,
                    info = map,
                    device = DeviceUtils.getDeviceType())
            userLogService.save(userLogEntity)
            return ResultCode.SUCCESS
    }
    @SaCheckRole("user")
    @PostMapping("/kb")
    fun kbPost(userLogPostRequest: UserLogPostRequest): Any
    {
        if(!userLogPostRequest.action.startsWith("kb."))
        {
            return ResultCode.PARAM_INVALID
        }
        if (UserLogEntity.validAction(userLogPostRequest.action))
        {
            return ResultCode.PARAM_INVALID
        }
        //kb的info是需要id的
        //构建
        val map=mapOf("id" to userLogPostRequest.id)
        val userId=StpUtil.getLoginIdAsLong()
        val userLogEntity = UserLogEntity(userId = userId,
                    action = userLogPostRequest.action,
                    info = map,
                    device = DeviceUtils.getDeviceType())
            userLogService.save(userLogEntity)
            return ResultCode.SUCCESS
    }

}