package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.common.config.interceptor.StpInterfaceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.common.util.user.DeviceUtils
import com.cainsgl.user.dto.request.UserLoginRequest
import com.cainsgl.user.dto.response.ErrorLoginResponse
import com.cainsgl.user.dto.response.LoginResponse
import com.cainsgl.user.service.CaptchaService
import com.cainsgl.user.service.UserOAuthServiceImpl
import com.cainsgl.user.service.UserServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import jakarta.validation.Valid
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/user")
class UserAuthController
{

    private val passwordEncoder = BCryptPasswordEncoder()

    @Resource
    lateinit var userService: UserServiceImpl

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Resource
    lateinit var captchaService: CaptchaService
    @Resource
    lateinit var userOAuthService: UserOAuthServiceImpl

    @PostMapping("/login")
    fun login(@RequestBody @Valid loginRequest: UserLoginRequest): Any
    {
        if (loginRequest.password.length < 6)
        {
            return ResultCode.PARAM_INVALID
        }
        val account = loginRequest.account
        if (captchaService.needCaptcha(account))
        {
            if (!captchaService.verifyCaptcha(account, loginRequest.captcha))
            {
                return ErrorLoginResponse(
                    msg = "验证码错误或已过期", needCaptcha = true,
                    captchaImage = captchaService.generateCaptcha(account)
                )
            }
        }

        // 查询用户并验证
        val user = userService.getUserByAccount(account)
        if (user == null || !passwordEncoder.matches(loginRequest.password, user.passwordHash))
        {
            // 记录登录失败并检查是否需要获取图片
            val captchaImage = if (captchaService.recordLoginFailure(account))
            {
                captchaService.generateCaptcha(account)
            } else null
            return ErrorLoginResponse(msg = "用户不存在或密码错误", needCaptcha =captchaImage!=null, captchaImage = captchaImage)
        }

        // 检查用户状态
        if (!user.isActive())
        {
            val extra: UserEntity.Extra? = userService.getExtra(user.id!!)
            val message = if (extra?.bannedTime == null)
            {

                "账户已被禁用"
            } else
            {
                "账户已被封禁至 ${extra.bannedTime}"
            }
            return  ErrorLoginResponse(msg = message, needCaptcha = false, captchaImage = null)
        }

        val device = DeviceUtils.getDeviceType()
        // 注销所有旧 Token
        val oldTokenList = StpUtil.getTokenValueListByLoginId(user.id, device)
        for (oldToken in oldTokenList)
        {
            StpUtil.logoutByTokenValue(oldToken)
        }

        StpUtil.login(user.id, device)
        // 保存角色和单独权限到 redis
        StpUtil.getSession().set(StpInterfaceImpl.USER_ROLE_KEY, user.roles)
        StpUtil.getSession().set(StpInterfaceImpl.USER_PERMISSIONS_KEY, user.permissions)

        log.info { "用户登录成功: userId=${user.id}, device=$device" }

        val token = StpUtil.getTokenValue()
        return LoginResponse(token, user.calculateLevelInfo().sanitizeSystemSensitiveData())
    }

    /**
     * 获取验证码
     * @param account 账号
     */
    @GetMapping("/auth/captcha")
    fun captcha(@RequestParam account: String): Any
    {
        if (account.isEmpty())
        {
            return ResultCode.PARAM_INVALID
        }
        val captchaImage = captchaService.generateCaptcha(account)
        return mapOf(
            "captchaImage" to captchaImage, "needCaptcha" to captchaService.needCaptcha(account)
        )
    }

    @PostMapping("/logout")
    fun logout(): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val device = DeviceUtils.getDeviceType()
        StpUtil.logout(userId, device)
        log.info { "用户登出成功: userId=$userId" }
        return ResultCode.SUCCESS
    }

    /**
     * @param request 包含 uid 的请求
     * @return 返回生成的 token
     */
    @GetMapping("/auth/o/bilibili")
    fun bilibiliOAuth(): Any
    {
        return userOAuthService.generateBilibiliToken()
    }
    @GetMapping("/auth/o/bilibili/fine")
    fun fineBilibiliOAuth(@RequestParam uid: String,@RequestParam token: String): Any
    {

        if(userOAuthService.verifyToken(token))
        {
            //toke存在，去检测对应的bilibili token
            val res= userOAuthService.verifyBilibiliToken(uid, token)
            if (userOAuthService.deleteBilibiliToken(token))
            {
                if(res is ErrorLoginResponse)
                {
                    //说明仍然出错，需要告诉用户刷新token了
                    res.token=userOAuthService.generateBilibiliToken()
                    res.modal="尝试次数过多，token已刷新"
                }
            }
            return res
        }
        return ErrorLoginResponse(msg="token已过期，已自动刷新",token=userOAuthService.generateBilibiliToken())
    }

}