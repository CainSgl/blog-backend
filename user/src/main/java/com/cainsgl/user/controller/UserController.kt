package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.user.dto.request.UpdateUserRequest
import com.cainsgl.user.dto.response.UserCurrentResponse
import com.cainsgl.user.dto.response.UserGetResponse
import com.cainsgl.user.service.UserExtraInfoServiceImpl
import com.cainsgl.user.service.UserServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*


private val log = KotlinLogging.logger {}
@RestController
@RequestMapping("/user")
class UserController
{
    val passwordEncoder = BCryptPasswordEncoder()
    @Resource
    lateinit var userExtraInfoService: UserExtraInfoServiceImpl
    @Resource
    lateinit var userService: UserServiceImpl


    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    fun getCurrentUser(): UserCurrentResponse
    {
        val userId=StpUtil.getLoginIdAsLong()
        val userInfo = userService.getById(userId)
        //获取自己的热信息
        val hotInfo = userExtraInfoService.getBySaveOnNull(userId)
        return  UserCurrentResponse(userInfo!!.calculateLevelInfo().sanitizeSystemSensitiveData(),hotInfo)
    }


    @GetMapping
    fun get(@RequestParam id: Long): Any
    {
        val user = userService.getById(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        //去除敏感字段
        user.calculateLevelInfo()
        val hotInfo = userExtraInfoService.getBySaveOnNull(user.id!!)
        return UserGetResponse(user,hotInfo)
    }

    @PutMapping
    fun update(@RequestBody request: UpdateUserRequest):Any
    {
        val userEntity=UserEntity(id=StpUtil.getLoginIdAsLong()).apply {
            nickname=request.nickname
            avatarUrl=request.avatarUrl
            bio=request.bio
            gender=request.gender
        }
        userService.updateById(userEntity)
        return ResultCode.SUCCESS
    }
    @GetMapping("/search")
    fun search(@RequestParam keyword: String): Any
    {
        //直接根据id搜索用户
        val user = userService.getById(keyword.toLongOrNull()?:0)
        if(user!=null)
        {
            return UserGetResponse(user,userExtraInfoService.getBySaveOnNull(user.id!!))
        }
        return ResultCode.RESOURCE_NOT_FOUND
    }
}
