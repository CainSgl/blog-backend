package com.cainsgl.user.controller

import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.annotation.SaIgnore
import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.common.config.interceptor.StpInterfaceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.common.exception.BusinessException
import com.cainsgl.common.util.DeviceUtils
import com.cainsgl.user.dto.request.UpdateUserRequest
import com.cainsgl.user.dto.request.UserLoginRequest
import com.cainsgl.user.dto.response.LoginResponse
import com.cainsgl.user.dto.response.UserCurrentResponse
import com.cainsgl.user.dto.response.UserGetResponse
import com.cainsgl.user.service.UserExtraInfoServiceImpl
import com.cainsgl.user.service.UserServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import jakarta.validation.Valid
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
    @SaIgnore
    @PostMapping("/login")
    fun login(@RequestBody @Valid loginRequest: UserLoginRequest): Any
    {
        // 查询用户并验证
        val user = userService.getUserByAccount(loginRequest.account) ?: throw BusinessException("用户不存在或密码错误")
        if (!passwordEncoder.matches(loginRequest.password, user.passwordHash))
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
        val device = DeviceUtils.getDeviceType()
        //注销所有旧 Token
        val oldTokenList = StpUtil.getTokenValueListByLoginId(user.id, device)
        for (oldToken in oldTokenList)
        {
            StpUtil.logoutByTokenValue(oldToken)
        }
        StpUtil.login(user.id, device)
        //保存角色和单独权限到redis
        StpUtil.getSession().set(StpInterfaceImpl.USER_ROLE_KEY, user.roles)
        StpUtil.getSession().set(StpInterfaceImpl.USER_PERMISSIONS_KEY, user.permissions)
        log.info { "${"用户登录成功: userId={}, device={}"} ${user.id} $device" }
        val token = StpUtil.getTokenValue()
        return LoginResponse(token, user.calculateLevelInfo().sanitizeSystemSensitiveData())
    }

    @SaCheckRole("user")
    @PostMapping("/logout")
    fun logout(): String
    {
        if (StpUtil.isLogin())
        {
            val userId = StpUtil.getLoginIdAsLong()
            val device = DeviceUtils.getDeviceType()
            StpUtil.logout(userId, device)
            log.info { "${"用户登出成功: userId={}"} $userId" }
        }
        return "登出成功"
    }

    /**
     * 获取当前登录用户信息
     */
    @SaCheckRole("user")
    @GetMapping("/current")
    fun getCurrentUser(): UserCurrentResponse
    {
        val userInfo = userService.getById(StpUtil.getLoginIdAsLong())
        //获取自己的热信息
        val hotInfo = userExtraInfoService.getBySaveOnNull(userInfo.id!!)
        return  UserCurrentResponse(userInfo.calculateLevelInfo().sanitizeSystemSensitiveData(),hotInfo)
    }

    @SaIgnore
    @GetMapping
    fun get(@RequestParam id: Long): Any
    {
        val user = userService.getById(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        //去除敏感字段
        user.calculateLevelInfo()
        val hotInfo = userExtraInfoService.getBySaveOnNull(user.id!!)
        return UserGetResponse(user,hotInfo)
    }
    @SaCheckRole("user")
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
}
