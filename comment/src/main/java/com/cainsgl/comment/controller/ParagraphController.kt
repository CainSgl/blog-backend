package com.cainsgl.comment.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.cainsgl.comment.dto.request.CreateParagraphRequest
import com.cainsgl.comment.entity.ParCommentEntity
import com.cainsgl.comment.entity.ParagraphEntity
import com.cainsgl.comment.service.ParCommentServiceImpl
import com.cainsgl.comment.service.ParagraphServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.util.user.UserHotInfoUtils.Companion.changeCommentCount
import com.cainsgl.senstitve.config.SensitiveWord
import jakarta.annotation.Resource
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/comment/paragraph")
class ParagraphController {

    @Resource
     lateinit var paragraphService: ParagraphServiceImpl
    @Resource
    lateinit var commentService: ParCommentServiceImpl
    @Resource
     lateinit var transactionTemplate: TransactionTemplate
    @Resource
    lateinit var sensitiveWord: SensitiveWord
    @Resource
    lateinit var redisTemplate: RedisTemplate<Any,Any>
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
        val content=sensitiveWord.replace(request.content)
        val comment=ParCommentEntity(userId=userId,dataId =request.dataId,version = request.version, postId = request.postId, content = content)
        
        var isParagraphExists = false
        
        // 第一个事务：尝试创建 paragraph
        transactionTemplate.execute {
            try {
                paragraphService.save(ParagraphEntity(postId = request.postId,dataId = request.dataId,version = request.version))
            } catch (e: DuplicateKeyException) {
                // 说明在阅读的时候，其他用户去创建他了
                isParagraphExists = true
                // 标记回滚以结束当前事务
                it.setRollbackOnly()
            }
        }
        
        // 第二个事务：保存评论
        return transactionTemplate.execute {
            commentService.save(comment)
            if (isParagraphExists) {
                // paragraph 已存在，只需增加计数
                paragraphService.incrementCount(postId = request.postId, version = request.version,dataId = request.dataId)
            } else {
                // 第一次创建，设置初始计数
                paragraphService.addCount(postId = request.postId, version = request.version,dataId = request.dataId,1)
                redisTemplate.changeCommentCount(1,userId)
            }
            ResultCode.SUCCESS
        }?: ResultCode.UNKNOWN_ERROR
    }

    @DeleteMapping("/comment")
    fun deleteParComment(@RequestParam id: Long): ResultCode
    {
        val userId = StpUtil.getLoginIdAsLong()
        val query = KtQueryWrapper(ParCommentEntity::class.java).eq(ParCommentEntity::id, id).eq(ParCommentEntity::userId, userId)
        transactionTemplate.execute {
            val entity = commentService.getOne(query) ?: return@execute
            commentService.removeById(id)
            paragraphService.addCount(postId = entity.postId!!, version = entity.version!!, dataId = entity.dataId!!, -1)
            paragraphService.incrementCount(postId = entity.postId!!, version = entity.version!!, dataId = entity.dataId!!, -1)
        }
        return ResultCode.SUCCESS
    }
}