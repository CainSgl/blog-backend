package com.cainsgl.comment.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.toolkit.IdWorker
import com.cainsgl.api.article.post.PostService
import com.cainsgl.api.user.UserService
import com.cainsgl.comment.dto.request.CreateParagraphRequest
import com.cainsgl.comment.dto.request.CreateReplyRequest
import com.cainsgl.comment.entity.ParCommentEntity
import com.cainsgl.comment.entity.ReplyEntity
import com.cainsgl.comment.service.ParCommentServiceImpl
import com.cainsgl.comment.service.ParagraphServiceImpl
import com.cainsgl.comment.service.PostsCommentServiceImpl
import com.cainsgl.comment.service.ReplyServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserNoticeType
import com.cainsgl.common.util.user.UserHotInfoUtils.Companion.changeCommentCount
import com.cainsgl.senstitve.config.SensitiveWord
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/comment")
class ParCommentController {
    @Resource
    private lateinit var parCommentService: ParCommentServiceImpl

    @Resource
    lateinit var paragraphService: ParagraphServiceImpl

    @Resource
    lateinit var transactionTemplate: TransactionTemplate

    @Resource
    lateinit var sensitiveWord: SensitiveWord

    @Resource
    lateinit var replyService: ReplyServiceImpl

    //其他模块的
    @Resource
    lateinit var postService: PostService
    @Resource
    lateinit var userService: UserService
    @Resource
    lateinit var postsCommentService: PostsCommentServiceImpl
    @Resource
    lateinit var redisTemplate: RedisTemplate<Any,Any>
    //创建评论
    @PostMapping
    fun createComment(@RequestBody request: CreateParagraphRequest): Any {
        if (request.content.length > 255) {
            return ResultCode.PARAM_INVALID
        }
        return transactionTemplate.execute { status ->
            val id = IdWorker.getId()
            if (!postService.addCommentCount(id = request.postId, 1)) {
                status.setRollbackOnly()
                return@execute null
            }
            val content = sensitiveWord.replace(request.content)
            paragraphService.incrementCount(postId = request.postId, version = request.version, dataId = request.dataId)
            val userId=StpUtil.getLoginIdAsLong()
            parCommentService.save(
                ParCommentEntity(
                    id = id,
                    userId = userId,
                    dataId = request.dataId,
                    version = request.version,
                    postId = request.postId,
                    content = content
                )
            )
            redisTemplate.changeCommentCount(1,userId)
            val res=HashMap<String,String>()
            res["id"] = id.toString()
            res["content"]=content?:""
            return@execute res
        } ?: "error"
    }

    //游标获取评论
    @GetMapping
    fun getComment(
        @RequestParam postId: Long,
        @RequestParam version: Int,
        @RequestParam dataId: Int,
        @RequestParam lastCreatedAt: LocalDateTime?,
        @RequestParam lastLikeCount: Int?,
        @RequestParam lastId: Long?
    ): List<ParCommentEntity> {
        return parCommentService.getByCursor(postId, version, dataId, lastCreatedAt, lastLikeCount, lastId)
    }

    @GetMapping("/reply")
    fun getReplyComment(
        @RequestParam postCommentId: Long?,
        @RequestParam parCommentId: Long?,
        lastCreatedAt: LocalDate?,
        lastLikeCount: Int?,
        lastId: Long?
    ): List<ReplyEntity> {

        if (postCommentId != null) {
            return replyService.getByPostCommentIdCursor(postCommentId, lastCreatedAt, lastLikeCount, lastId)
        }
        if (parCommentId != null) {
            return replyService.getByParCommentIdCursor(parCommentId, lastCreatedAt, lastLikeCount, lastId)
        }
        return emptyList()
    }

    @PostMapping("/reply")
    fun createReply(@RequestBody request: CreateReplyRequest):Any {
        if (request.content.length > 255) {
            return ResultCode.PARAM_INVALID
        }
        if (request.parCommentId == null && request.postCommentId == null) {
            return ResultCode.MISSING_PARAM
        }
        val id = IdWorker.getId()
        val content = sensitiveWord.replace(request.content)
        val userId=StpUtil.getLoginIdAsLong()
        val replyEntity = ReplyEntity(
            id = id,
            userId = userId,
            content = content,
            replyId = request.replyId,
            postCommentId = request.postCommentId,
            parCommentId = request.parCommentId
        )

        val entityId:String= transactionTemplate.execute { status ->
            if (!postService.addCommentCount(id = request.postId, 1)) {
                status.setRollbackOnly()
                return@execute null
            }
            if (request.parCommentId != null) {
                if (request.dataId == null) {
                    status.setRollbackOnly()
                    return@execute null
                }
                if (!parCommentService.addReplyCount(id = request.parCommentId, 1)) {
                    status.setRollbackOnly()
                    return@execute null
                }
                paragraphService.incrementCount(
                    postId = request.postId,
                    version = request.version,
                    dataId = request.dataId
                )
            } else {
                postsCommentService.addCommentCount(id = request.postCommentId!!, 1)
            }
            replyService.save(replyEntity)
            redisTemplate.changeCommentCount(1,userId)
            //MQ TODO ，可以靠mq优化
            userService.createNotice(id, UserNoticeType.REPLY.type, request.replyId,targetUser=userId)
            return@execute id.toString()
        } ?: "error"
        val res=HashMap<String,String>()
        res["id"] = entityId
        res["content"]=content?:""
        return res
    }
}