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
 * 文章发布消费者
 */
private val log = KotlinLogging.logger {}
@Component
@RocketMQMessageListener(
    consumerGroup = "article-add-consumer",
    topic = "article",
    tag = "publish",
    consumptionThreadCount=15
)
class ArticlePublishConsumer : BaseConsumer<Long>(Long::class.java) {

//    @Resource
//    lateinit var postCloneGrpcService: PostCloneGrpcService
    @Resource
    lateinit var postHistoryService: PostHistoryService
    @Resource
    lateinit var postService: PostService
    @Resource
    lateinit var postChunkVectorService: PostChunkVectorService

    override fun doConsume(message: Long, messageView: MessageView): ConsumeResult {
        // 发布了，同步到clone里去
        val postId=message
        //获取旧数据，方便后续重新加载向量不用全量加载
        val oldData = postHistoryService.getLastById(postId)
        //从向量数据库删除原来可能存在的，并重新发布向量，这里就是差量更新了，具体实现跟这里就无关了
        postChunkVectorService.reloadVector(postId,oldData?.content)
        log.info { "消费者成功让文档：$postId,向量化" }
        return ConsumeResult.SUCCESS
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
