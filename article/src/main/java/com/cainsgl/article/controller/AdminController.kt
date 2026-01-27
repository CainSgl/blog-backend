package com.cainsgl.article.controller

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
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

    @PostMapping("/loadAll")
    fun loadAll(): Any
    {
        var lastId: Long = 0
        var hasMore = true
        while (hasMore)
        {
            // 从数据库中获取一批文章数据
            val query = QueryWrapper<PostEntity>().select("id", "title", "content", "summary", "tags", "img", "status")
                .orderByAsc("id").gt("id", lastId).ge("status", ArticleStatus.PUBLISHED).last("limit 100")
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
}