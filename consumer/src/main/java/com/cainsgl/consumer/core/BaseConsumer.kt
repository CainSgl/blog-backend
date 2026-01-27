package com.cainsgl.consumer.core

import com.alibaba.fastjson2.JSON
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.apache.rocketmq.client.apis.consumer.ConsumeResult
import org.apache.rocketmq.client.apis.message.MessageView
import org.apache.rocketmq.client.core.RocketMQListener
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration


private val log =KotlinLogging.logger {  }
abstract class BaseConsumer<T : Any>(private val messageType: Class<T>) : RocketMQListener
{

    companion object
    {
        private const val IDEMPOTENT_KEY_PREFIX = "mq:consumed:"
        private val IDEMPOTENT_EXPIRE: Duration = Duration.ofHours(24)
    }
    @Resource
    protected lateinit var redisTemplate: RedisTemplate<String, Any>
    protected open val enableIdempotent: Boolean = false
    protected open val idempotentExpire: Duration = IDEMPOTENT_EXPIRE

    protected open val consumerName=this::class.simpleName!!

    override fun consume(messageView: MessageView): ConsumeResult
    {
        val messageId = messageView.messageId.toString()
        return try
        {
            // 幂等性检查
            if (enableIdempotent && isDuplicate(messageId))
            {
                log.warn { "[${consumerName()}] 重复消息，跳过处理: ,messageId=${messageId}" }
                return ConsumeResult.SUCCESS
            }
            val message = parseMessage(messageView)
            val result = doConsume(message, messageView)
            //标记已消费
            if (enableIdempotent && result == ConsumeResult.SUCCESS)
            {
                markConsumed(messageId)
            }
            result
        } catch (e: NonRetryableException)
        {
            // 不可重试异常
            log.error { "[${consumerName()}] 不可重试异常，消息将被丢弃,messageId=${messageId}" }
            ConsumeResult.SUCCESS
        } catch (e: Exception)
        {
            // 可重试异常
            log.error { "[${consumerName()}] 消费异常 ， 将重试,messageId=${messageId}" }
            ConsumeResult.FAILURE
        }
    }

    /**
     * 业务处理逻辑，子类实现
     */
    protected abstract fun doConsume(message: T, messageView: MessageView): ConsumeResult

    /**
     * 消费者名称，用于日志标识
     */
    protected open fun consumerName(): String = consumerName

    /**
     * 解析消息体
     */
    private fun parseMessage(messageView: MessageView): T
    {
        val bodyBytes = ByteArray(messageView.body.remaining())
        messageView.body.get(bodyBytes)
        val bodyString = String(bodyBytes, Charsets.UTF_8)

        return when (messageType)
        {
            String::class.java                             -> @Suppress("UNCHECKED_CAST") (bodyString as T)
            Long::class.java, java.lang.Long::class.java   -> @Suppress("UNCHECKED_CAST") (bodyString.toLong() as T)
            Int::class.java, Integer::class.java -> @Suppress("UNCHECKED_CAST") (bodyString.toInt() as T)
            else                                           -> JSON.parseObject(bodyString, messageType)
        }
    }

    /**
     * 检查是否重复消息
     */
    private fun isDuplicate(messageId: String): Boolean
    {
        val redis = redisTemplate
        val key = "$IDEMPOTENT_KEY_PREFIX$messageId"
        return redis.hasKey(key) == true
    }

    /**
     * 标记消息已消费
     */
    private fun markConsumed(messageId: String)
    {
        val redis = redisTemplate
        val key = "$IDEMPOTENT_KEY_PREFIX$messageId"
        redis.opsForValue().set(key, "1", idempotentExpire)


    }
}

/**
 * 不可重试异常
 * 抛出此异常表示消息无需重试，直接标记成功
 */
class NonRetryableException(message: String, data: Any?=null) : RuntimeException(message)
{
    private var data:Any?=null
    init
    {
        this.data=data
    }
}
