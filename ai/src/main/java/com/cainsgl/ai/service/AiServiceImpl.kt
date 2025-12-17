package com.cainsgl.ai.service


import com.alibaba.fastjson2.JSON
import com.cainsgl.ai.dto.AiMessage
import com.cainsgl.ai.dto.TagCore
import com.cainsgl.ai.utils.use
import com.cainsgl.api.ai.AiService
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.util.VectorUtils
import com.volcengine.ark.runtime.service.ArkService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Duration

private val logger = KotlinLogging.logger {}

@Service
class AiServiceImpl : AiService
{
    @Value("\${dimensions}")
    var dimensions: Int = 0


    @Resource
    lateinit var embeddingModel: EmbeddingModel

    @Resource
    lateinit var arkService: ArkService

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    companion object
    {
        const val AI_CHAT_REDIS_PREFIX = "ai:chat:"
        const val AI_CHAT_NUM_REDIS_PREFIX = "ai:number:"
        val GENERATE_TAG_PROMPT = """
            你是擅长分析文本内容并生成准确相关标签的专家，请严格按以下步骤执行：
            任务：分析用户后续提供的文章内容，生成主题标签。
            标签语言：所有标签必须为简体中文。
            内容处理规则：
            1. 仅基于用户后续提供的文本进行分析，不推断、不添加文本中未包含的任何信息。
            2. 无需处理任何格式符号，直接以用户提供的纯文本为分析依据。
            标签生成标准：
            1. 最多生成10个标签。
            2. 标签按与内容核心主题的相关性降序排列（最相关在前）。
            3. 每个标签需分配0-1之间的置信度分数，分数反映该标签概念与内容的核心关联程度。
            4. 所有标签的分数总和必须等于1.0。
            输出格式：仅返回一个列表，格式严格如下（无其他文本、解释或格式）：
            ["标签1", "分数", "标签2", "分数", ...]
        """.trimIndent()
        val DEFAULT_PROMPT = """
            你是来自一个个人博客的聊天AI，名字叫小C，你只提供帮助，拒绝与用户闲聊，拒绝聊与博客网站不相关的。
        """.trimIndent()
    }

    override fun getEmbedding(text: String): FloatArray
    {
        //降低到对应的维度，并且归一化
        val embed = embeddingModel.embed(text)
        return VectorUtils.reduceDimension(embed, dimensions)
    }

    override fun getEmbedding(texts: List<String>): List<FloatArray>
    {
        val embeds = embeddingModel.embed(texts)
        return VectorUtils.reduceDimensionBatch(embeds, dimensions)
    }

    fun getTagsByContent(content: String): List<TagCore>
    {
        val res = arkService.use()
            .systemMsg(GENERATE_TAG_PROMPT)
            .userMsg(content)
            .noThinKing()
            .send()
        val tagCoreList = mutableListOf<TagCore>()
        val array = JSON.parseArray(res)
        array.chunked(2).forEach { chunk ->
            val tagName = chunk[0].toString().replace("\"", "")
            val core = chunk[1].toString().replace("\"", "").toFloat()
            tagCoreList.add(TagCore(tagName, core))
        }
        return tagCoreList
    }

    fun chat(content: String, emitter: SseEmitter, userId: Long)
    {
        val redisKey = "$AI_CHAT_NUM_REDIS_PREFIX$userId"
        val currentCount = redisTemplate.opsForValue().increment(redisKey) ?: 0L
        if (currentCount == 1L)
        {
            redisTemplate.expire(redisKey, Duration.ofDays(1))
        }
        //TODO 目前直接限制为10次
        if (currentCount > 10)
        {
            emitter.send(com.cainsgl.common.dto.response.Result.success("请求达到限制了"))
            emitter.complete()
            return
        }
        redisTemplate.opsForList().rightPush("$AI_CHAT_REDIS_PREFIX$userId", AiMessage("User", content))
        Thread.ofVirtual().start {
            try
            {
                val sb = StringBuilder()
                arkService.use()
                    .systemMsg(DEFAULT_PROMPT)
                    .userMsg(content)
                    .thinKing()
                    .send({ res ->
                        emitter.send(res.choices)
                        sb.append(res.choices[0].message.content)
                    },
                        onError = {
                            logger.warn { it }
                            //可能是用户的输入达到上限了
                            emitter.send(com.cainsgl.common.dto.response.Result.fromCode(ResultCode.AI_TOO_MAY_REQUESTS))
                            emitter.complete()
                        },
                        onComplete = {
                            //保存记录到redis
                            emitter.complete()
                            val msg = sb.toString()
                            if (msg.isNotEmpty())
                            {

                                redisTemplate.opsForList()
                                    .rightPush("$AI_CHAT_REDIS_PREFIX$userId", AiMessage("Ai", sb.toString()), Duration.ofDays(3))
                            }
                        })
            } catch (e: Exception)
            {
                logger.error { e }
            }
        }
    }

}
