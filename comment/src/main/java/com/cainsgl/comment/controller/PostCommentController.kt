package com.cainsgl.comment.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.toolkit.IdWorker
import com.cainsgl.api.article.post.PostService
import com.cainsgl.comment.dto.request.CommentPostRequest
import com.cainsgl.comment.entity.PostsCommentEntity
import com.cainsgl.comment.service.PostsCommentServiceImpl
import jakarta.annotation.Resource
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/post/comment")
class PostCommentController
{
    @Resource
    lateinit var postsCommentService: PostsCommentServiceImpl

    @Resource
    lateinit var transactionTemplate: TransactionTemplate

    //其他模块的
    @Resource
    lateinit var postService: PostService

    @GetMapping
    fun getByCursor(
        @RequestParam postId: Long, @RequestParam lastCreatedAt: LocalDateTime?,
        @RequestParam lastLikeCount: Int?,@RequestParam lastId: Long?
    ): List<PostsCommentEntity>
    {
        return postsCommentService.getByCursor(postId, lastCreatedAt, lastLikeCount,lastId)
    }

    @PostMapping
    fun create(@RequestBody request: CommentPostRequest): String
    {
        val id = IdWorker.getId()
        return transactionTemplate.execute {
            postsCommentService.save(
                PostsCommentEntity(
                    id = id,
                    content = request.content,
                    userId = StpUtil.getLoginIdAsLong(),
                    version = request.version,
                    postId = request.postId
                )
            )
            postService.addCommentCount(request.postId, 1)
            return@execute id.toString()
        } ?: "error"
    }
}