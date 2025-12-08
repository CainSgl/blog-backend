package com.cainsgl.consumer.article

import com.cainsgl.api.article.post.PostService
import jakarta.annotation.Resource
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
    tag = "publish"
)
class ArticlePublishConsumer : RocketMQListener
{
    private val log = LoggerFactory.getLogger(this::class.java)
    @Resource
    lateinit var postService: PostService

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
    tag = "content"
)
class ArticleContentConsumer : RocketMQListener
{
    private val log = LoggerFactory.getLogger(this::class.java)

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
    private val log = LoggerFactory.getLogger(this::class.java)
    override fun consume(messageView: MessageView): ConsumeResult
    {
        return ConsumeResult.SUCCESS
    }
}
