package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.cainsgl.common.config.interceptor.StpInterfaceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.common.util.user.DeviceUtils
import com.cainsgl.user.dto.request.RegisterRequest
import com.cainsgl.user.dto.request.UserLoginRequest
import com.cainsgl.user.dto.response.ErrorLoginResponse
import com.cainsgl.user.dto.response.LoginResponse
import com.cainsgl.user.dto.response.RegisterSuccessResponse
import com.cainsgl.user.service.CaptchaService
import com.cainsgl.user.service.UserOAuthServiceImpl
import com.cainsgl.user.service.UserServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import jakarta.validation.Valid
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/user")
class UserAuthController
{

    private val passwordEncoder = BCryptPasswordEncoder()

    @Resource
    lateinit var userService: UserServiceImpl

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @Resource
    lateinit var captchaService: CaptchaService

    @Resource
    lateinit var userOAuthService: UserOAuthServiceImpl

    companion object
    {

        const val REGISTER_USERNAME_REDIS_PREFIX_KEY = "register:username:"
    }

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): Any
    {
        if (request.step == null)
        {
            //续期token
            if (request.token == null)
            {
                return ResultCode.PARAM_INVALID
            }
            val redisKey = "${UserOAuthServiceImpl.REGISTER_TOKEN}${request.token}"
            val map =
                redisTemplate.opsForValue().getAndExpire(redisKey, 30, TimeUnit.SECONDS) as? Map<String?,String?> ?: return ErrorLoginResponse(
                    msg = "token已过期，请刷新"
                )
            redisTemplate.expire(REGISTER_USERNAME_REDIS_PREFIX_KEY + map["username"], 30, TimeUnit.SECONDS)
            return ResultCode.SUCCESS
        }
        if (request.step == 1)
        {
            request.username?.trim()
            //检测username是否有效
            if (request.username == null || request.username!!.length < 4)
            {
                return ErrorLoginResponse("用户名不能为空，且用户名至少大于4")
            }
            //生成token并返回
            //去检验该用户名是否合法
            val query = KtQueryWrapper(UserEntity::class.java).eq(UserEntity::username, request.username)
            val exist = redisTemplate.opsForValue().setIfAbsent(
                REGISTER_USERNAME_REDIS_PREFIX_KEY + request.username, request.username, 30L, TimeUnit.SECONDS
            )
            if (exist != true)
            {
                //去检测下是不是当前用户在注册
                val v = userOAuthService.getByToken(UserOAuthServiceImpl.REGISTER_TOKEN, request.token!!)
                    ?: return ErrorLoginResponse("token过期，请重新注册")
                val map = v as? MutableMap<String?, String?> ?: return ErrorLoginResponse("token过期，请重新注册")
                if(map["username"] != request.username)
                {
                    return ErrorLoginResponse("该用户正在注册")
                }
            }
            if (userService.exists(query))
            {
                return ErrorLoginResponse("该用户名已存在")
            }
            val token = userOAuthService.generateToken(
                UserOAuthServiceImpl.REGISTER_TOKEN, value = mutableMapOf("username" to request.username), expireTime = 30L
            )
            if(request.token!=null)
            {
                //清空那个数据
                userOAuthService.removeByToken(UserOAuthServiceImpl.REGISTER_TOKEN, request.token!!)
            }
            return RegisterSuccessResponse(token = token)
        }
        if (request.token == null)
        {
            return ResultCode.PARAM_INVALID
        }
        if (request.step == 2)
        {
            val v = userOAuthService.getByToken(UserOAuthServiceImpl.REGISTER_TOKEN, request.token!!)
                ?: return ErrorLoginResponse("token过期，请重新注册")
            val map = v as MutableMap<String?, String?>
            map["email"] = request.email
            userOAuthService.setByToken(UserOAuthServiceImpl.REGISTER_TOKEN, request.token!!,map,30L)
            return ResultCode.SUCCESS
        }
        if(request.step == 3)
        {
            val v = userOAuthService.getByToken(UserOAuthServiceImpl.REGISTER_TOKEN, request.token!!)
                ?: return ErrorLoginResponse("token过期，请重新注册")
            val map = v as? MutableMap<String?, String?> ?: return ErrorLoginResponse("token过期，请重新注册")
            map["password"] = request.password
            userOAuthService.setByToken(UserOAuthServiceImpl.REGISTER_TOKEN, request.token!!,map,30L)
            return ResultCode.SUCCESS
        }
        if(request.step == 4)
        {
            //完成注册
            val v = userOAuthService.getByToken(UserOAuthServiceImpl.REGISTER_TOKEN, request.token!!)
                ?: return ErrorLoginResponse("token过期，请重新注册")
            val map = v as? MutableMap<String?, String?> ?: return ErrorLoginResponse("token过期，请重新注册")

            val user=  userService.crateUserBaseInfo(UserEntity().apply {
                passwordHash= passwordEncoder.encode(map["password"]).trim()
                username = map["username"]!!.trim()
                email = map["email"]?.trim()
            })
            StpUtil.login(user.id, DeviceUtils.getDeviceType())
            // 保存角色和单独权限到 redis
            StpUtil.getSession().set(StpInterfaceImpl.USER_ROLE_KEY, user.roles)
            StpUtil.getSession().set(StpInterfaceImpl.USER_PERMISSIONS_KEY, user.permissions)
            return LoginResponse(token=StpUtil.getTokenValue(),null,true)
        }
        return ResultCode.SUCCESS
    }

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
            return ErrorLoginResponse(
                msg = "用户不存在或密码错误", needCaptcha = captchaImage != null, captchaImage = captchaImage
            )
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
            return ErrorLoginResponse(msg = message, needCaptcha = false, captchaImage = null)
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
        return userOAuthService.generateToken()
    }

    @GetMapping("/auth/o/bilibili/fine")
    fun fineBilibiliOAuth(@RequestParam uid: String, @RequestParam token: String): Any
    {

        if (userOAuthService.verifyToken(token))
        {
            //toke存在，去检测对应的bilibili token
            val res = userOAuthService.verifyBilibiliToken(uid, token)
            if (userOAuthService.deleteBilibiliToken(token))
            {
                if (res is ErrorLoginResponse)
                {
                    //说明仍然出错，需要告诉用户刷新token了
                    res.token = userOAuthService.generateToken()
                    res.modal = "尝试次数过多，token已刷新"
                }
            }
            return res
        }
        return ErrorLoginResponse(msg = "token已过期，已自动刷新", token = userOAuthService.generateToken())
    }

}