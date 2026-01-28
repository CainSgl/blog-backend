package com.cainsgl.user.service

import cn.dev33.satoken.stp.StpUtil
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONPath
import com.baomidou.mybatisplus.core.toolkit.IdWorker
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.config.interceptor.StpInterfaceImpl
import com.cainsgl.common.entity.user.OAuthType
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.common.entity.user.UserOAuthEntity
import com.cainsgl.common.util.user.DeviceUtils
import com.cainsgl.user.dto.response.ErrorLoginResponse
import com.cainsgl.user.dto.response.LoginResponse
import com.cainsgl.user.repository.UserOAuthMapper
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class UserOAuthServiceImpl : ServiceImpl<UserOAuthMapper, UserOAuthEntity>(), IService<UserOAuthEntity>
{
    @Resource
    private lateinit var userService: UserServiceImpl

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @Resource
    lateinit var restClient: RestClient


    companion object
    {
        private const val BILIBILI_TOKEN_PREFIX = "oauth:bilibili:token:"
        private const val TOKEN_EXPIRE_MINUTES = 5L // token 有效期 5 分钟
        private const val OAUTH_SUCESS_REGSTER = "oauth:sucess:token:"
    }

    /**
     * 为 Bilibili OAuth 生成验证 token
     * @param uid Bilibili 用户 ID
     * @return 生成的 token
     */
    fun generateBilibiliToken(prefix: String = BILIBILI_TOKEN_PREFIX, value: Any =5): String
    {
        var token: String;
        while (true)
        {
            token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().toByteArray(StandardCharsets.UTF_8))
            val redisKey = "$prefix$token"
            //碰撞概率极低，这里只是为了万一，所以偷懒不写lua了
            if (redisTemplate.hasKey(redisKey) == true)
            {
                continue;
            }
            redisTemplate.opsForValue().set(redisKey, value, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES)
            break;
        }
        return token
    }

    fun verifyToken(token: String): Boolean
    {
        val redisKey = "$BILIBILI_TOKEN_PREFIX$token"
        val count = redisTemplate.opsForValue().get(redisKey) as? Number
        return count != null && count.toLong() > 0
    }

    @Transactional
    fun verifyBilibiliToken(uid: String, token: String): Any
    {

        val bodyStr = restClient.get().uri("https://uapis.cn/api/v1/social/bilibili/userinfo?uid=${uid}").retrieve()
            .onStatus({ status -> status.isError }) { _, response ->
                // 不抛出异常，让后续代码处理响应体
            }
            .body<String>()
        val sign = JSONPath.eval(bodyStr, "sign")
        if (sign == null)
        {
            val error = JSONPath.eval(bodyStr, "error")?.toString()
            return if (error == "Bilibili user not found")
            {
                ErrorLoginResponse(msg = "请检验你的uid是否正确，找不到这样的bilibili用户")
            } else
            {
                ErrorLoginResponse(msg = "发生了一个异常，因为${error}")
            }
        }
        if (sign.toString().contains(token))
        {
            val parseObject: Map<*, *> = JSON.parseObject(bodyStr, Map::class.java)
            //直接创建账号并登录，方便前端
            val oauth = createOAuthOrGet(parseObject["uid"].toString(), OAuthType.BILIBILI)
            var user = userService.getById(oauth.userId)
            val isNew=user==null
            if(isNew)
            {
                user= userService.crateUserBaseInfo(UserEntity().apply {
                        bio = sign.toString().replace(token, "")
                        nickname = parseObject["name"].toString()
                        gender = parseObject["sex"].toString()

                    })
            }
            StpUtil.login(user.id, DeviceUtils.getDeviceType())
            StpUtil.getSession().set(StpInterfaceImpl.USER_ROLE_KEY, user.roles)
            StpUtil.getSession().set(StpInterfaceImpl.USER_PERMISSIONS_KEY, user.permissions)
            val token=StpUtil.getTokenValue()
            return LoginResponse(token,user.calculateLevelInfo().sanitizeSystemSensitiveData(),isNew)
        } else
        {
            return ErrorLoginResponse(msg = "请检验bilibili的个人签名是否包含对应的token，你的签名是：$sign")
        }
    }


    fun createOAuthOrGet(providerUserId: String, type: OAuthType): UserOAuthEntity
    {
        val query = KtQueryWrapper(UserOAuthEntity::class.java).eq(
            UserOAuthEntity::providerUserId, providerUserId
        ).eq(UserOAuthEntity::provider, type.value)
        val userOAuth = getOne(query)
        if (userOAuth == null)
        {
            val genId = IdWorker.getId()
            //去创建
            val entity = UserOAuthEntity()
            entity.providerUserId = providerUserId
            entity.provider = type.value
            entity.id = genId
            save(entity)
            return entity
        }
        return userOAuth
    }

    fun deleteBilibiliToken(token: String): Boolean
    {
        val redisKey = "$BILIBILI_TOKEN_PREFIX$token"
        val c = redisTemplate.opsForValue().decrement(redisKey) ?: return true
        if(c<=0)
        {
            if(c!=-1L)
            {
                redisTemplate.delete(redisKey)
            }
            //刷新
            return true;
        }
        return false
    }
}
