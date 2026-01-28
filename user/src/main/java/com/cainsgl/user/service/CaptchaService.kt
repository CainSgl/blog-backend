package com.cainsgl.user.service

import com.wf.captcha.SpecCaptcha
import com.wf.captcha.base.Captcha
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Service
class CaptchaService {
    
    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>
    
    companion object {
        private const val CAPTCHA_PREFIX = "captcha:"
        private const val LOGIN_FAIL_PREFIX = "login:fail:"
        private const val CAPTCHA_EXPIRE_TIME = 1L // 验证码1分钟过期
        private const val FAIL_COUNT_EXPIRE_TIME = 10L // 失败计数10分钟过期
        private const val CAPTCHA_WIDTH = 150
        private const val CAPTCHA_HEIGHT = 50
        private const val CAPTCHA_LENGTH = 4
    }

    fun generateCaptcha(account: String): String {
        val captcha = SpecCaptcha(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_LENGTH)
        captcha.setCharType(Captcha.TYPE_DEFAULT)
        val code = captcha.text().lowercase()
        val key = getCaptchaKey(account)
        redisTemplate.opsForValue().set(key, code, CAPTCHA_EXPIRE_TIME, TimeUnit.MINUTES)
        log.debug { "生成验证码: account=$account, code=$code" }
        return captcha.toBase64()
    }
    
    /**
     * 验证验证码
     * @param account 账号
     * @param captcha 用户输入的验证码
     * @return 是否验证通过
     */
    fun verifyCaptcha(account: String, captcha: String?): Boolean {
        if (captcha.isNullOrBlank()) {
            return false
        }
        
        val key = getCaptchaKey(account)
        val storedCode = redisTemplate.opsForValue().get(key)
        
        if (storedCode==null) {
            log.warn { "验证码不存在或已过期: account=$account" }
            return false
        }
        
        val isValid = storedCode.toString().equals(captcha.trim().lowercase(), ignoreCase = true)
        
        if (isValid) {
            // 验证成功后删除验证码
            redisTemplate.delete(key)
            log.info { "验证码验证成功: account=$account" }
        } else {
            log.warn { "验证码验证失败: account=$account, input=$captcha, expected=$storedCode" }
        }
        
        return isValid
    }
    

    fun recordLoginFailure(account: String): Boolean {
        val key = getLoginFailKey(account)
        val count = redisTemplate.opsForValue().increment(key) ?: 1L
        redisTemplate.expire(key, FAIL_COUNT_EXPIRE_TIME, TimeUnit.MINUTES)
        log.info { "记录登录失败: account=$account, count=$count" }
        //这里是去自增后再去判断的，所以需要多1
        return count>=3
    }

    fun needCaptcha(account: String): Boolean {
        val key = getLoginFailKey(account)
        val count = redisTemplate.opsForValue().get(key) ?: return false
        val numberCount=count as Int
        return numberCount>=2
    }

    private fun getCaptchaKey(account: String): String {
        return "$CAPTCHA_PREFIX$account"
    }

    private fun getLoginFailKey(account: String): String {
        return "$LOGIN_FAIL_PREFIX$account"
    }
}
