package com.cainsgl.ai.utils

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChunk
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole
import com.volcengine.ark.runtime.service.ArkService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.reactivex.Flowable


fun ArkService.use(): SimpleArkApiUser
{
    return SimpleArkApiUser(this)
}


enum class Model(val modelName: String)
{
    DEFAULT("doubao-seed-1-6-lite-251015"),

    //支持思考的长短啥的
    LITE("doubao-seed-1-6-lite-251015")
}

enum class ThinKing(val thinKing: String)
{
    AUTO("auto"),
    DISABLE("disabled"),
    ENABLE("enabled")
}

private val log = KotlinLogging.logger { }

class SimpleArkApiUser(private val arkService: ArkService)
{
    private val messages = ArrayList<ChatMessage>()
    private var model: String = Model.DEFAULT.modelName
    private var thinKing: String = ""
    private var maxToken: Int = 32768
    private var maxCompletionToken: Int =32768
    fun systemMsg(msg: String): SimpleArkApiUser
    {
        addMsg(ChatMessageRole.SYSTEM, msg)
        return this
    }

    fun maxToken(maxToken: Int): SimpleArkApiUser
    {
        this.maxToken = maxToken
        return this
    }
    fun maxCompletionToken(maxToken:Int): SimpleArkApiUser
    {
        this.maxCompletionToken = maxToken
        return this
    }
    fun userMsg(msg: String): SimpleArkApiUser
    {
        addMsg(ChatMessageRole.USER, msg)
        return this
    }

    fun assistantMsg(msg: String): SimpleArkApiUser
    {
        addMsg(ChatMessageRole.ASSISTANT, msg)
        return this
    }

    fun model(model: Model): SimpleArkApiUser
    {
        this.model = model.modelName
        return this
    }

    fun thinKing(): SimpleArkApiUser
    {
        this.thinKing = "enabled"
        return this
    }

    fun thinKingByAuto(): SimpleArkApiUser
    {
        this.thinKing = "auto"
        return this
    }

    fun noThinKing(): SimpleArkApiUser
    {
        this.thinKing = "disabled"
        return this
    }

    private fun addMsg(role: ChatMessageRole, content: String)
    {
        val userMessage = ChatMessage.builder()
            .role(role)
            .content(content)
            .build()
        messages.add(userMessage)
    }

    fun send(onNext: (ChatCompletionChunk) -> Unit, onError: (Throwable) -> Unit = { log.error { it } }, onComplete: () -> Unit = {})
    {
        val req = ChatCompletionRequest.builder()
            .model(model)
            .stream(true)
            .messages(messages)
            .apply {
                if (thinKing.isNotEmpty())
                {
                    thinking(ChatCompletionRequest.ChatCompletionRequestThinking(thinKing))
                }
                if (maxToken > 0)
                {
                    maxTokens(maxToken)
                }else if (maxCompletionToken > 0)
                {
                    maxCompletionTokens(maxCompletionToken)
                }
            }
        val streamChatCompletion: Flowable<ChatCompletionChunk> = arkService.streamChatCompletion(req.build())
        streamChatCompletion.subscribe(onNext, onError, onComplete)
    }

    fun send(): String
    {
        val req = ChatCompletionRequest.builder()
            .model(model)
            .stream(false)
            .messages(messages)
            .apply {
                if (thinKing.isNotEmpty())
                {
                    thinking(ChatCompletionRequest.ChatCompletionRequestThinking(thinKing))
                }
                if (maxToken > 0)
                {
                    maxTokens(maxToken)
                } else if (maxCompletionToken > 0)
                {
                    maxCompletionTokens(maxCompletionToken)
                }
            }

        return arkService.createChatCompletion(req.build()).choices[0].message.content.toString()
    }
}

class DefaultRe