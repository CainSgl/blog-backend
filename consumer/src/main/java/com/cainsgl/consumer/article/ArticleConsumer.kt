package com.cainsgl.consumer.article

import com.cainsgl.api.article.post.history.PostHistoryService
import com.cainsgl.api.article.post.PostService
import com.cainsgl.api.article.post.chunk.PostChunkVectorService
import com.cainsgl.consumer.core.BaseConsumer
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.apache.rocketmq.client.annotation.RocketMQMessageListener
import org.apache.rocketmq.client.apis.consumer.ConsumeResult
import org.apache.rocketmq.client.apis.message.MessageView
import org.springframework.stereotype.Component

/**
 * 文章发布消息DTO
 */
data class PostPublishMessage(
    val postId: Long,
    val historyId: Long,
    val userId: Long,
    val version: Int,
    val content: String,
    val title: String,
    val summary: String?,
    val img: String?,
    val tags: List<String>?
)

/**
 * 文章发布消费者
 */
private val log = KotlinLogging.logger {}
@Component
@RocketMQMessageListener(
    consumerGroup = "article-publish-consumer",
    topic = "article",
    tag = "publish",
    consumptionThreadCount=15
)
class ArticlePublishConsumer : BaseConsumer<PostPublishMessage>(PostPublishMessage::class.java) {

    @Resource
    lateinit var postHistoryService: PostHistoryService
    @Resource
    lateinit var postService: PostService
    @Resource
    lateinit var postChunkVectorService: PostChunkVectorService

    override fun doConsume(message: PostPublishMessage, messageView: MessageView): ConsumeResult {
        try {
            // 1. 更新历史版本
            postHistoryService.updateById(message.historyId, message.content)
            log.info { "更新历史版本成功, historyId=${message.historyId}" }
            
            // 2. 创建新的历史版本供作者继续编辑
            postHistoryService.createNewVersion(
                userId = message.userId,
                postId = message.postId,
                version = message.version + 1,
                content = message.content
            )
            log.info { "创建新历史版本成功, postId=${message.postId}, version=${message.version + 1}" }
            
            // 3. 保存到ES文档（通过gRPC调用）
            if (message.content.isNotEmpty()) {
                postService.saveToElasticsearch(
                    postId = message.postId,
                    title = message.title,
                    summary = message.summary,
                    img = message.img,
                    content = message.content,
                    tags = message.tags
                )
                log.info { "保存到ES成功, postId=${message.postId}" }
            }
            
            // 4. 延时双删缓存
            Thread.sleep(1000)
            postService.removeCache(message.postId)
            log.info { "清除缓存成功, postId=${message.postId}" }
            
            // 5. 重新加载向量
            val oldData = postHistoryService.getLastById(message.postId)
            postChunkVectorService.reloadVector(message.postId, oldData?.content)
            log.info { "向量化成功, postId=${message.postId}" }
            
            return ConsumeResult.SUCCESS
        } catch (e: Exception) {
            log.error(e) { "消费文章发布消息失败, postId=${message.postId}" }
            return ConsumeResult.FAILURE
        }
    }
}

/**
 * 文章内容更新消费者
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "article-update-consumer",
    topic = "article",
    tag = "content",
    consumptionThreadCount=5
)
class ArticleContentConsumer : BaseConsumer<Long>(Long::class.java) {
//    @Resource
//    lateinit var postCloneGrpcService: PostCloneGrpcService
    @Resource
    lateinit var postService: PostService
    @Resource
    lateinit var postChunkVectorService: PostChunkVectorService
    override fun doConsume(message: Long, messageView: MessageView): ConsumeResult {
        log.info { "消费者监听到id为$message 的文章变动，但不是发布的文章" }
        return ConsumeResult.SUCCESS
    }
}

/**
 * 文章删除消费者
 */
@Component
@RocketMQMessageListener(
    consumerGroup = "article-delete-consumer",
    topic = "article",
    tag = "delete",
    consumptionThreadCount=5
)
class ArticleDeleteConsumer : BaseConsumer<Long>(Long::class.java) {
//    @Resource
//    lateinit var postCloneGrpcService: PostCloneGrpcService
    @Resource
    lateinit var postChunkVectorService: PostChunkVectorService
    override fun doConsume(message: Long, messageView: MessageView): ConsumeResult {
        log.info("消费文章删除消息，id=$message")
//        //删除clone里的，以及chunk
//        postCloneGrpcService.delete( message)
        postChunkVectorService.removeVector(message)
        return ConsumeResult.SUCCESS
    }
}
