package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.cainsgl.common.dto.response.Result
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.user.dto.request.UpdateUserRequest
import com.cainsgl.user.dto.response.UserCurrentResponse
import com.cainsgl.user.dto.response.UserGetResponse
import com.cainsgl.user.service.UserExtraInfoServiceImpl
import com.cainsgl.user.service.UserServiceImpl
import com.cainsgl.user.service.CheckInServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*


private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/user")
class UserController
{


    @Resource
    lateinit var userExtraInfoService: UserExtraInfoServiceImpl

    @Resource
    lateinit var userService: UserServiceImpl

    @Resource
    lateinit var checkInService: CheckInServiceImpl


    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    fun getCurrentUser(): UserCurrentResponse
    {
        val userId = StpUtil.getLoginIdAsLong()
        val userInfo = userService.getById(userId)
        //获取自己的热信息
        val hotInfo = userExtraInfoService.getBySaveOnNull(userId)
        return UserCurrentResponse(userInfo!!.calculateLevelInfo().sanitizeSystemSensitiveData(), hotInfo)
    }


    @GetMapping
    fun get(@RequestParam id: Long): Any
    {
        val user = userService.getById(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        //去除敏感字段
        user.calculateLevelInfo()
        val hotInfo = userExtraInfoService.getBySaveOnNull(user.id!!)
        return UserGetResponse(user, hotInfo)
    }

    @PutMapping
    fun update(@RequestBody request: UpdateUserRequest): Any
    {
        if (request.username != null)
        {
            //返回
            val userId = StpUtil.getLoginIdAsLong()
            val update = KtUpdateWrapper(UserEntity::class.java).eq(UserEntity::id, userId).isNull(UserEntity::username)
                .ne(UserEntity::username, request.username).set(UserEntity::username, request.username)
            return if (userService.update(update))
            {
                ResultCode.SUCCESS
            } else
            {
                Result.error("用户名重复，请重新输入")
            }
        }
        val userEntity = UserEntity(id = StpUtil.getLoginIdAsLong()).apply {
            nickname = request.nickname
            avatarUrl = request.avatarUrl
            bio = request.bio
            gender = request.gender
        }
        userService.updateById(userEntity)
        return ResultCode.SUCCESS
    }

    @GetMapping("/search")
    fun search(@RequestParam keyword: String): Any
    {
        //直接根据id搜索用户
        val user = userService.getById(keyword.toLongOrNull() ?: 0)
        if (user != null)
        {
            return UserGetResponse(user, userExtraInfoService.getBySaveOnNull(user.id!!))
        }
        return ResultCode.RESOURCE_NOT_FOUND
    }

    /**
     * 用户签到
     */
    @PostMapping("/checkin")
    fun checkIn(): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        return checkInService.checkIn(userId)
    }
}
