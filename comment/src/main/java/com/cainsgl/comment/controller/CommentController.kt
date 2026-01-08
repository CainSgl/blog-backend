package com.cainsgl.comment.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.toolkit.IdWorker
import com.cainsgl.comment.dto.request.CreateParagraphRequest
import com.cainsgl.comment.entity.CommentEntity
import com.cainsgl.comment.service.CommentServiceImpl
import com.cainsgl.comment.service.ParagraphServiceImpl
import jakarta.annotation.Resource
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/comment")
class CommentController
{
    @Resource
    private lateinit var commentService: CommentServiceImpl

    @Resource
    lateinit var paragraphService: ParagraphServiceImpl

    @Resource
    lateinit var transactionTemplate: TransactionTemplate

    //创建评论
    @PostMapping
    fun createComment(@RequestBody request: CreateParagraphRequest): String
    {
        return transactionTemplate.execute {
            val id=IdWorker.getId()
            paragraphService.incrementCount(postId = request.postId, version = request.version, dataId = request.dataId)
            commentService.save(
                CommentEntity(
                    id=id,
                    userId = StpUtil.getLoginIdAsLong(),
                    dataId = request.dataId,
                    version = request.version,
                    postId = request.postId,
                    content = request.content
                )
            )
            return@execute id.toString()
        }?:"error"
    }

    //游标获取评论
    @GetMapping
    fun getComment(
        @RequestParam postId: Long,
        @RequestParam version: Int,
        @RequestParam dataId: Int,
        @RequestParam lastCreatedAt: LocalDate?,
        @RequestParam lastLikeCount: Int?
    ): List<CommentEntity>
    {
        return commentService.getByCursor(postId, version, dataId, lastCreatedAt, lastLikeCount)

    }
}