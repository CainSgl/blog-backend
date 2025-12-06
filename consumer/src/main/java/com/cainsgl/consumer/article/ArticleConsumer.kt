package com.cainsgl.consumer.article

import org.apache.rocketmq.client.annotation.RocketMQMessageListener
import org.apache.rocketmq.client.apis.consumer.ConsumeResult
import org.apache.rocketmq.client.apis.message.MessageView
import org.apache.rocketmq.client.core.RocketMQListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@RocketMQMessageListener(
    endpoints = "rocketmq-proxy:8081",
    consumerGroup = "article-add-consumer",
    topic = "article",
    tag = "post"
)
class ArticleAddConsumer : RocketMQListener
{
    private val log = LoggerFactory.getLogger(ArticleAddConsumer::class.java)


    override fun consume(messageView: MessageView): ConsumeResult
    {



        return   ConsumeResult.FAILURE
         //   val articleId = String(messageView.body.array())
    }
}


@Component
@RocketMQMessageListener(
    endpoints = "rocketmq-proxy:8081",
    consumerGroup = "article-update-consumer",
    topic = "article",
    tag = "update"
)
class ArticleUpdateConsumer : RocketMQListener
{
    private val log = LoggerFactory.getLogger(ArticleUpdateConsumer::class.java)

    override fun consume(messageView: MessageView): ConsumeResult
    {
        println("hi,mq")
        return ConsumeResult.SUCCESS
    }
}

@Component
@RocketMQMessageListener(
    endpoints = "rocketmq-proxy:8081",
    consumerGroup = "article-delete-consumer",
    topic = "article",
    tag = "delete"
)
class ArticleDeleteConsumer : RocketMQListener
{
    private val log = LoggerFactory.getLogger(ArticleDeleteConsumer::class.java)
    override fun consume(messageView: MessageView): ConsumeResult
    {
        return ConsumeResult.SUCCESS
    }
}
