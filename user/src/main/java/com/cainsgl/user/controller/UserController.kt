package com.cainsgl.user.controller

import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.user.dto.request.LoginRequest
import com.cainsgl.user.dto.response.LoginResponse
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.common.exception.BusinessException
import com.cainsgl.common.util.UserUtils
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
    lateinit var userService: UserServiceImpl
    /**
     * 用户登录
     */
    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): Any
    {
        if (!loginRequest.validate())
        {
            return ResultCode.MISSING_PARAM
        }
        // 查询用户并验证

        val user = userService.getUserByAccount(loginRequest.account!!) ?: throw BusinessException("用户不存在或密码错误")
        if (!passwordEncoder.matches(loginRequest.password!!, user.passwordHash))
        {
            throw BusinessException("用户不存在或密码错误")
        }
        // 检查用户状态
        if (!user.isActive())
        {
            val extra: UserEntity.Extra? = userService.getExtra(user.id!!);
            val message = if (extra?.bannedTime == null)
            {
                "账户已被禁用"
            } else
            {
                "账户已被封禁至 ${extra.bannedTime}"
            }
            throw BusinessException(message)
        }

        val device = LoginRequest.getDeviceType()
        //注销所有旧 Token
        val oldTokenList = StpUtil.getTokenValueListByLoginId(user.id, device)
        for (oldToken in oldTokenList)
        {
            StpUtil.logoutByTokenValue(oldToken)
        }
        StpUtil.login(user.id, device)
        UserUtils.setUserInfo(user)
        log.info { "${"用户登录成功: userId={}, device={}"} ${user.id} $device" }
        val token = StpUtil.getTokenValue()
        return LoginResponse(token, user)
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    fun logout(): Any
    {
        if (StpUtil.isLogin())
        {
            val userId = StpUtil.getLoginIdAsLong()
            StpUtil.logout()
            log.info { "${"用户登出成功: userId={}"} $userId" }
        }
        return "登出成功"
    }

    /**
     * 获取当前登录用户信息
     */
    @SaCheckRole("user")
    @GetMapping("/current")
    fun getCurrentUser(): Any
    {
        if (!StpUtil.isLogin())
        {
            throw BusinessException("未登录")
        }
        return UserUtils.getUserInfo()!!
    }

    @GetMapping
    fun get(@RequestParam(required = false) id: Long?): Any
    {
        if(id==null)
        {
            return ResultCode.MISSING_PARAM
        }
        val userEntity = UserEntity()
        userEntity.username = "test123"
        userService.save(userEntity)
        val user = userService.getUser(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        return user
    }


}
