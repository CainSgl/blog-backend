package com.cainsgl.common.annotation

import com.cainsgl.common.dto.response.Result
import com.cainsgl.common.dto.response.ResultCode
import jakarta.annotation.Resource
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

/**
 * 该注解必须要引入redis才生效
 * @param interval 一个周期的时间范围，单位是毫秒
 * @param limit 代表一个周期内的限制次数
 * @param message 返回给用户的信息，为空代表默认
 * @author cainsgl
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimitByToken(
    val interval: Long,
    val message: String = "",
    val tokenHeaderKey: String = "token",
    val limit: Int,
)

@Component
@ConditionalOnClass(RedisTemplate::class)
class TokenRateLimiter
{
    companion object
    {
        private const val RATE_LIMIT_KEY_PREFIX = "rate_limit:"
    }

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    fun checkAccess(method: String, key: String, interval: Long, limit: Int): Boolean
    {
        val redisKey = "$RATE_LIMIT_KEY_PREFIX$method:$key"
        //TODO 这里是有可能突然崩溃导致redis的key始终无法释放，可以用lua脚本或者后续扫描解决
        val currentCount = redisTemplate.opsForValue().increment(redisKey, 1)
        if (currentCount == 1L) {
            redisTemplate.expire(redisKey, interval, TimeUnit.MILLISECONDS)
        }
        return if (currentCount != null)
        {
            currentCount <= limit
        }else
        {
            //异常
            false
        }
    }
}

@Aspect
@Component
@ConditionalOnClass(RedisTemplate::class)
class TokenRateLimitAspect
{
    @Resource
    lateinit var rateLimiter: TokenRateLimiter

    @Pointcut("@annotation(com.cainsgl.common.annotation.RateLimitByToken)")
    fun tokenRateLimitPointcut()
    {
    }

    @Around("tokenRateLimitPointcut()")
    @Throws(Throwable::class)
    fun around(joinPoint: ProceedingJoinPoint): Any
    {
        val signature: MethodSignature = joinPoint.signature as MethodSignature
        val method: Method = signature.method
        val annotation: RateLimitByToken = method.getAnnotation(RateLimitByToken::class.java)
        val methodName: String = method.name
        val interval: Long = annotation.interval
        val limit: Int = annotation.limit
        val tokenHeaderKey = annotation.tokenHeaderKey
        val attributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
        val request = attributes.request
        val token: String = request.getHeader(tokenHeaderKey)
        if (!rateLimiter.checkAccess(methodName, token, interval, limit))
        {
            val message: String = annotation.message
            return if (message.isEmpty())
            {
                ResultCode.TOO_MANY_REQUESTS
            } else
            {
                Result(ResultCode.TOO_MANY_REQUESTS.code, message, null)
            }

        }
        return joinPoint.proceed()
    }

}