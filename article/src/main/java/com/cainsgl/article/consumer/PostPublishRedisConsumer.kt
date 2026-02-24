package com.cainsgl.article.consumer

import com.cainsgl.article.document.PostDocument
import com.cainsgl.article.dto.PostPublishMessage
import com.cainsgl.article.service.PostChunkVectorServiceImpl
import com.cainsgl.article.service.PostDocumentService
import com.cainsgl.article.service.PostHistoryServiceImpl
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.entity.article.PostHistoryEntity
import com.cainsgl.common.mq.redis.RedisMessageConsumer
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(name = ["mq.type"], havingValue = "redis", matchIfMissing = false)
class PostPublishRedisConsumer : RedisMessageConsumer {

    @Resource
    lateinit var postHistoryService: PostHistoryServiceImpl

    @Resource
    lateinit var postDocumentService: PostDocumentService

    @Resource
    lateinit var postService: PostServiceImpl

    @Resource
    lateinit var postChunkVectorService: PostChunkVectorServiceImpl

    @Resource
    lateinit var objectMapper: ObjectMapper

    override fun topic(): String = "article:publish"

    override fun onMessage(message: String) {
        try {
            val msg = objectMapper.readValue(message, PostPublishMessage::class.java)
            log.info { "收到文章发布消息: postId=${msg.postId}" }

            // 更新历史版本
            postHistoryService.updateById(
                PostHistoryEntity(
                    id = msg.historyId,
                    content = msg.content,
                    userId = msg.userId
                )
            )

            // 创建新的历史版本供作者下次编辑使用
            postHistoryService.save(
                PostHistoryEntity(
                    userId = msg.userId,
                    postId = msg.postId,
                    version = msg.version + 1,
                    createdAt = LocalDateTime.now(),
                    content = msg.content
                )
            )
            if (msg.content.isNotEmpty()) {
                postDocumentService.save(
                    PostDocument(
                        id = msg.postId,
                        title = msg.title,
                        summary = msg.summary,
                        img = msg.img,
                        content = msg.content,
                        tags = msg.tags
                    )
                )
            }
            Thread.ofVirtual().start {
                Thread.sleep(1000)
                try {
                    postService.removeCache(msg.postId)
                } catch (e: Exception) {
                    log.error(e) { "延时清除缓存失败, postId=${msg.postId}" }
                }
            }
            val oldData = postHistoryService.getLastById(msg.postId)
            postChunkVectorService.reloadVector(msg.postId, oldData?.content)
            log.info { "向量化成功, postId=${msg.postId}" }
            log.info { "文章发布消息处理完成: postId=${msg.postId}" }
        } catch (e: Exception) {
            log.error(e) { "处理文章发布消息失败: $message" }
        }
    }
}
