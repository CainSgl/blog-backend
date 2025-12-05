package com.cainsgl.common.util
import org.apache.rocketmq.client.apis.message.*
import org.apache.rocketmq.client.java.message.MessageBuilderImpl

object MqUtils
{
    private val messageBuilder:MessageBuilderImpl=MessageBuilderImpl()
    @JvmStatic
    fun createMessage(bytes:ByteArray):Message
    {
        val message: Message = messageBuilder.setTopic("article")
            .setKeys("messageKey")
            .setTag("add")
            .setBody(bytes)
            .build()
        return message
    }
}