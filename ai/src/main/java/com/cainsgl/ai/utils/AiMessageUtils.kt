package com.cainsgl.ai.utils

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole
import com.volcengine.ark.runtime.service.ArkService
import java.util.function.Consumer

fun ArkService.use(): SimpleArkApiUser
{
    return SimpleArkApiUser(this)
}


enum class Model(val modelName: String)
{
    DEFAULT("doubao-seed-1-6-lite-251015"),
    LITE("doubao-seed-1-6-lite-251015")
}

enum class ThinKing(val thinKing: String)
{
    AUTO("auto"),
    DISABLE("disabled"),
    ENABLE("enabled")
}

class SimpleArkApiUser(private val arkService: ArkService)
{
    private val messages = ArrayList<ChatMessage>()
    private var model: String = Model.DEFAULT.modelName
    private var thinKing: String = ""
    fun systemMsg(msg: String): SimpleArkApiUser
    {
        addMsg(ChatMessageRole.SYSTEM, msg)
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

    fun model(model: Model):SimpleArkApiUser
    {
        this.model = model.modelName
        return this
    }
    fun thinKing():SimpleArkApiUser
    {
        this.thinKing = "enabled"
        return this
    }
    fun thinKingByAuto():SimpleArkApiUser
    {
        this.thinKing = "auto"
        return this
    }
    fun noThinKing():SimpleArkApiUser
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

    fun send(consumer: Consumer<ChatCompletionChoice>)
    {
        val req = ChatCompletionRequest.builder()
            .model(model)
            .stream(true)
            .maxCompletionTokens(65535)
            .messages(messages)
        if(thinKing.isNotEmpty())
        {
            req.thinking(ChatCompletionRequest.ChatCompletionRequestThinking(thinKing))
        }
        arkService.createChatCompletion(req.build()).choices.forEach(consumer)
    }
    fun send():String
    {
        val req = ChatCompletionRequest.builder()
            .model(model)
            .stream(false)
            .maxCompletionTokens(65535)
            .messages(messages)
        if(thinKing.isNotEmpty())
        {
            req.thinking(ChatCompletionRequest.ChatCompletionRequestThinking(thinKing))
        }
         return arkService.createChatCompletion(req.build()).choices[0].message.content.toString()
    }
}

class DefaultRe