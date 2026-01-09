package com.cainsgl.comment.controller

import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.comment.dto.request.CreateParagraphRequest
import com.cainsgl.comment.entity.ParCommentEntity
import com.cainsgl.comment.entity.ParagraphEntity
import com.cainsgl.comment.service.ParCommentServiceImpl
import com.cainsgl.comment.service.ParagraphServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import jakarta.annotation.Resource
import org.springframework.dao.DuplicateKeyException
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/paragraph")
class ParagraphController {

    @Resource
     lateinit var paragraphService: ParagraphServiceImpl
    @Resource
    lateinit var commentService: ParCommentServiceImpl
    @Resource
     lateinit var transactionTemplate: TransactionTemplate

    @GetMapping("/comment")
    fun getCountByPost(@RequestParam id:Long,@RequestParam version: Int):List<ParagraphEntity>
    {
        return paragraphService.getCountByPost(id,version)?: emptyList()
    }
    //这是创建首个评论
    @PostMapping("/comment")
    fun createParagraph(@RequestBody request: CreateParagraphRequest):ResultCode
    {
        val userId=StpUtil.getLoginIdAsLong()
        val comment=ParCommentEntity(userId=userId,dataId =request.dataId,version = request.version, postId = request.postId, content = request.content)
        return transactionTemplate.execute {
            try {
                paragraphService.save(ParagraphEntity(postId = request.postId,dataId = request.dataId,version = request.version))
            } catch (e: DuplicateKeyException) {
                // 说明在阅读的时候，其他用户去创建他了，直接去用redis自增评论数量即可
                commentService.save(comment)
                paragraphService.incrementCount(postId = request.postId, version = request.version,dataId = request.dataId)
                return@execute ResultCode.SUCCESS
            }
            // 说明确实是第一次创建的
            commentService.save(comment)
            paragraphService.setCount(postId = request.postId, version = request.version,dataId = request.dataId,1)
            return@execute ResultCode.SUCCESS
        }?: ResultCode.UNKNOWN_ERROR
    }
}