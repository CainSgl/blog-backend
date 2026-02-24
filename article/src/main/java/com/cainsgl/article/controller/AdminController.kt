package com.cainsgl.article.controller

import cn.dev33.satoken.annotation.SaCheckRole
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.cainsgl.article.document.PostDocument
import com.cainsgl.article.service.PostDocumentService
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.PostEntity
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/post")
class AdminController
{
    @Resource
    lateinit var postDocumentService: PostDocumentService

    @Resource
    lateinit var postService: PostServiceImpl

    @Resource
    lateinit var postChunkVectorService: com.cainsgl.article.service.PostChunkVectorServiceImpl
    @SaCheckRole("admin")
    @PostMapping("/loadAll")
    fun loadAll(): Any
    {
        var lastId: Long = 0
        var hasMore = true
        while (hasMore)
        {
            val query = KtQueryWrapper(PostEntity::class.java).select(PostEntity::id, PostEntity::title, PostEntity::content, PostEntity::summary, PostEntity::tags, PostEntity::img, PostEntity::status)
                .orderByAsc(PostEntity::id).gt(PostEntity::id, lastId).ge(PostEntity::status, ArticleStatus.PUBLISHED).last("limit 100")
            val posts = postService.list(query)
            if (posts.isEmpty())
            {
                break
            }
            // 将PostEntity转换为PostDocument并保存到ES
            val postDocuments = posts.filter {
                it.status == ArticleStatus.PUBLISHED || it.status == ArticleStatus.ONLY_FANS
            }.map { post ->
                PostDocument(
                    id = post.id!!,
                    title = post.title ?: "",
                    content = post.content,
                    summary = post.summary,
                    tags = post.tags ?: emptyList(), img = post.img, score = 0.0
                )
            }

            // 批量保存到Elasticsearch
            if (postDocuments.isNotEmpty())
            {
                postDocumentService.saveAll(postDocuments)
            }
            if(posts.isEmpty())
            {
                break
            }
            // 更新lastId以进行下一次分页查询
            lastId = posts.last().id!!
            hasMore = posts.size == 100
        }
        return mapOf("success" to true, "message" to "All posts loaded to ES successfully")
    }

    @SaCheckRole("admin")
    @PostMapping("/loadAllVectors")
    fun loadAllVectors(): Any
    {
        var lastId: Long = 0
        var processedCount = 0
        var successCount = 0
        var failedCount = 0

        while (true)
        {
            val query = KtQueryWrapper(PostEntity::class.java)
                .select(PostEntity::id, PostEntity::content, PostEntity::status)
                .orderByAsc(PostEntity::id)
                .gt(PostEntity::id, lastId)
                .ge(PostEntity::status, ArticleStatus.PUBLISHED)
                .last("limit 50")
            
            val posts = postService.list(query)
            if (posts.isEmpty())
            {
                break
            }

            posts.forEach { post ->
                try {
                    if (post.content.isNullOrEmpty()) {
                        return@forEach
                    }
                    
                    // 调用loadVector方法进行向量化
                    val success = postChunkVectorService.loadVector(post.id!!)
                    if (success) {
                        successCount++
                    } else {
                        failedCount++
                    }
                    processedCount++
                } catch (e: Exception) {
                    failedCount++
                    processedCount++
                }
            }

            lastId = posts.last().id!!
            
            if (posts.size < 50) {
                break
            }
        }

        return mapOf(
            "success" to true,
            "message" to "批量向量化完成",
            "processed" to processedCount,
            "success" to successCount,
            "failed" to failedCount
        )
    }
}