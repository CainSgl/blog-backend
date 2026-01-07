package com.cainsgl.comment.controller

import com.cainsgl.comment.dto.request.CreateParagraphRequest
import com.cainsgl.comment.entity.CommentEntity
import com.cainsgl.comment.service.CommentServiceImpl
import com.cainsgl.comment.service.ParagraphServiceImpl
import com.cainsgl.common.dto.response.ResultCode
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
    fun createParagraph(@RequestBody request: CreateParagraphRequest): ResultCode
    {
        transactionTemplate.execute {
            paragraphService.incrementCount(postId = request.postId, version = request.version, dataId = request.dataId)
            commentService.save(
                CommentEntity(
                    dataId = request.dataId,
                    version = request.version,
                    postId = request.postId,
                    content = request.content
                )
            )
        }
        return ResultCode.SUCCESS
    }

    //游标获取评论
    @GetMapping
    fun getComment(
        @RequestParam postId: Int,
        @RequestParam version: Int,
        @RequestParam dataId: Int,
        @RequestParam lastCreatedAt: LocalDate?,
        @RequestParam lastLikeCount: Int?
    ): List<CommentEntity>
    {
        return commentService.getByCursor(postId, version, dataId, lastCreatedAt, lastLikeCount)

    }
}