package com.cainsgl.consumer.article

import com.cainsgl.api.article.post.PostService
import com.cainsgl.api.article.post.chunk.PostChunkVectorService
import com.cainsgl.api.go.PostCloneGrpcService
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.consumer.core.BaseConsumer
import com.cainsgl.consumer.core.NonRetryableException
import jakarta.annotation.Resource
import org.apache.rocketmq.client.annotation.RocketMQMessageListener
import org.apache.rocketmq.client.apis.consumer.ConsumeResult
import org.apache.rocketmq.client.apis.message.MessageView
import org.springframework.stereotype.Component

/**
 * 文章发布消费者
 */
@Component
@RocketMQMessageListener(
    endpoints = "rocketmq-proxy:8080",
    consumerGroup = "article-add-consumer",
    topic = "article",
    tag = "publish",
    consumptionThreadCount=15
)
class ArticlePublishConsumer : BaseConsumer<Long>(Long::class.java) {

    @Resource
    lateinit var postCloneGrpcService: PostCloneGrpcService

    @Resource
    lateinit var postService: PostService
    @Resource
    lateinit var postChunkVectorService: PostChunkVectorService

    override fun doConsume(message: Long, messageView: MessageView): ConsumeResult {
        // 发布了，同步到clone里去
        val postId=message
        //可能是后续在消费的时候用户又把删除了，所以这里就不管他，直接抛出异常
        val postEntity = postService.getById(postId) ?: throw NonRetryableException("文章不存在", postId)
        //先获取旧数据，这里是发布，大概率是没有旧数据的，不过为了防止某些人恶意的发布又删除
        val oldData = postCloneGrpcService.getPostById(postId)
        postCloneGrpcService.upsertPost(postId,postEntity.title?:"",postEntity.content?:"",postEntity.status?: ArticleStatus.PUBLISHED)
        //从向量数据库删除原来可能存在的，并重新发布向量，这里就是差量更新了，具体实现跟这里就无关了
        postChunkVectorService.reloadVector(postId,oldData?.content)
        log.info("消费者成功让文档：$postId,向量化")
        return ConsumeResult.SUCCESS
    }
}

/**
 * 文章内容更新消费者
 */
@Component
@RocketMQMessageListener(
    endpoints = "rocketmq-proxy:8080",
    consumerGroup = "article-update-consumer",
    topic = "article",
    tag = "content",
    consumptionThreadCount=5
)
class ArticleContentConsumer : BaseConsumer<Long>(Long::class.java) {
    @Resource
    lateinit var postCloneGrpcService: PostCloneGrpcService
    @Resource
    lateinit var postService: PostService
    @Resource
    lateinit var postChunkVectorService: PostChunkVectorService
    override fun doConsume(message: Long, messageView: MessageView): ConsumeResult {
        val postId=message
        val postEntity = postService.getById(postId) ?: throw NonRetryableException("文章不存在", postId)
        //发布中的文章，更新向量
        if (postEntity.status == ArticleStatus.PUBLISHED) {
            val oldData = postCloneGrpcService.getPostById(postId)
            if(postEntity.content!=oldData?.content){
                postCloneGrpcService.upsertPost(postId,postEntity.title?:"",postEntity.content?:"",postEntity.status?: ArticleStatus.PUBLISHED)
                postChunkVectorService.reloadVector(postId,oldData?.content)
                log.info("消费者成功让文档：$postId,向量化")
            }
        }
        return ConsumeResult.SUCCESS
    }
}

/**
 * 文章删除消费者
 */
@Component
@RocketMQMessageListener(
    endpoints = "rocketmq-proxy:8080",
    consumerGroup = "article-delete-consumer",
    topic = "article",
    tag = "delete",
    consumptionThreadCount=5
)
class ArticleDeleteConsumer : BaseConsumer<Long>(Long::class.java) {
    @Resource
    lateinit var postCloneGrpcService: PostCloneGrpcService
    @Resource
    lateinit var postChunkVectorService: PostChunkVectorService
    override fun doConsume(message: Long, messageView: MessageView): ConsumeResult {
        log.info("消费文章删除消息，id=$message")
        //删除clone里的，以及chunk
        postCloneGrpcService.delete( message)
        //TODO
        return ConsumeResult.SUCCESS
    }
}
