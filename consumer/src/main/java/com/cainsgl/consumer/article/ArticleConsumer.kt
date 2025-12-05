package com.cainsgl.consumer.article

import org.apache.rocketmq.client.annotation.RocketMQMessageListener
import org.apache.rocketmq.client.apis.consumer.ConsumeResult
import org.apache.rocketmq.client.apis.message.MessageView
import org.apache.rocketmq.client.core.RocketMQListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@RocketMQMessageListener(
    endpoints = "rocketmq-nameserver:9876",
    consumerGroup = "article-add-consumer",
    topic = "article",
    tag = "add"

)
class ArticleAddConsumer : RocketMQListener
{
    private val log = LoggerFactory.getLogger(ArticleAddConsumer::class.java)

    override fun consume(messageView: MessageView): ConsumeResult
    {
        return try
        {
            val articleId = String(messageView.body.array())
            log.info("收到文章新增消息，文章ID: {}", articleId)
            // TODO: 处理文章新增逻辑，例如同步到ES
            ConsumeResult.SUCCESS
        } catch (e: Exception)
        {
            log.error("处理文章新增消息失败", e)
            ConsumeResult.FAILURE
        }
    }
}


@Component
@RocketMQMessageListener(
    endpoints = "rocketmq-nameserver:9876",
    consumerGroup = "article-update-consumer",
    topic = "article",
    tag = "update"
)
class ArticleUpdateConsumer : RocketMQListener
{
    private val log = LoggerFactory.getLogger(ArticleUpdateConsumer::class.java)

    override fun consume(messageView: MessageView): ConsumeResult
    {
        return try
        {
            val articleId = String(messageView.body.array())
            log.info("收到文章更新消息，文章ID: {}", articleId)

            // TODO: 处理文章更新逻辑，例如更新ES索引

            ConsumeResult.SUCCESS
        } catch (e: Exception)
        {
            log.error("处理文章更新消息失败", e)
            ConsumeResult.FAILURE
        }
    }
}

@Component
@RocketMQMessageListener(
    endpoints = "rocketmq-nameserver:9876",
    consumerGroup = "article-delete-consumer",
    topic = "article",
    tag = "delete"
)
class ArticleDeleteConsumer : RocketMQListener
{
    private val log = LoggerFactory.getLogger(ArticleDeleteConsumer::class.java)
    override fun consume(messageView: MessageView): ConsumeResult
    {
        return try
        {
            val articleId = String(messageView.body.array())
            log.info("收到文章删除消息，文章ID: {}", articleId)

            // TODO: 处理文章删除逻辑，例如删除ES索引

            ConsumeResult.SUCCESS
        } catch (e: Exception)
        {
            log.error("处理文章删除消息失败", e)
            ConsumeResult.FAILURE
        }
    }
}
