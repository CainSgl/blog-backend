package com.cainsgl.comment.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.toolkit.IdWorker
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.cainsgl.api.article.util.ChangePostCommentCount
import com.cainsgl.api.user.UserService
import com.cainsgl.comment.dto.request.CreateReplyRequest
import com.cainsgl.comment.dto.request.NoticeReplyResponse
import com.cainsgl.comment.entity.ParCommentEntity
import com.cainsgl.comment.entity.PostsCommentEntity
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
import java.time.LocalDateTime

@RestController
@RequestMapping("/comment/reply")
class ReplyController
{
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
    lateinit var changePostCommentCount: ChangePostCommentCount

    @Resource
    lateinit var userService: UserService

    @Resource
    lateinit var postsCommentService: PostsCommentServiceImpl

    @Resource
    lateinit var redisTemplate: RedisTemplate<Any, Any>

    @GetMapping
    fun getReplyComment(
        @RequestParam postCommentId: Long?, @RequestParam parCommentId: Long?, lastCreatedAt: LocalDateTime?,
        lastLikeCount: Int?, lastId: Long?
    ): List<ReplyEntity>
    {

        if (postCommentId != null)
        {
            return replyService.getByPostCommentIdCursor(postCommentId, lastCreatedAt, lastLikeCount, lastId)
        }
        if (parCommentId != null)
        {
            return replyService.getByParCommentIdCursor(parCommentId, lastCreatedAt, lastLikeCount, lastId)
        }
        return emptyList()
    }

    @GetMapping("/getMyReply")
    fun getMyReply(@RequestParam id: Long): ReplyEntity
    {
        val query = KtQueryWrapper(ReplyEntity::class.java).eq(ReplyEntity::id, id)
        return replyService.getOne(query)
    }
    @DeleteMapping
    fun deleteReply(@RequestParam id: Long): ResultCode
    {
        val userId= StpUtil.getLoginIdAsLong();
        val query= KtQueryWrapper(ReplyEntity::class.java).eq(ReplyEntity::id, id).eq(ReplyEntity::userId, userId);
        transactionTemplate.execute {
            val entity = replyService.getOne(query) ?: return@execute
            if(entity.postCommentId != null)
            {
                //文章评论，-1
                postsCommentService.addReplyCount(entity.postCommentId!!,-1);
            }else if(entity.parCommentId != null)
            {
                parCommentService.addReplyCount(entity.parCommentId!!,-1);
            }

        }
        return ResultCode.SUCCESS;
    }

    @GetMapping("/notice")
    fun getReplyForNotice(@RequestParam ids: List<Long>): List<NoticeReplyResponse>
    {
        //返回基础信息即可
        val query = KtQueryWrapper(ReplyEntity::class.java).select(
            ReplyEntity::id, ReplyEntity::parCommentId, ReplyEntity::postCommentId, ReplyEntity::content,
            ReplyEntity::likeCount, ReplyEntity::createdAt, ReplyEntity::replyCommentId,
        ).`in`(ReplyEntity::id, ids.distinct())
        val replyList = replyService.list(query)
        //还需要获取他的回复对象
        val parCommentIds = replyList.mapNotNull { it.parCommentId }.distinct()
        val postCommentIds = replyList.mapNotNull { it.postCommentId }.distinct()
        val replyCommentIds = replyList.mapNotNull { it.replyCommentId }.distinct()
        //去找到对应的所有被回复的评论信息
        val parCommentContentMap = if (parCommentIds.isNotEmpty())
        {
            val parQuery =
                KtQueryWrapper(ParCommentEntity::class.java).select(ParCommentEntity::id, ParCommentEntity::content)
                    .`in`(ParCommentEntity::id, parCommentIds)
            parCommentService.list(parQuery).associateBy({ it.id }, { it.content })
        } else
        {
            emptyMap()
        }
        val postCommentContentMap = if (postCommentIds.isNotEmpty())
        {
            val postQuery = KtQueryWrapper(PostsCommentEntity::class.java).select(
                PostsCommentEntity::id, PostsCommentEntity::content
            ).`in`(PostsCommentEntity::id, postCommentIds)
            postsCommentService.list(postQuery).associateBy({ it.id }, { it.content })
        } else
        {
            emptyMap()
        }
        val replyCommentContentMap = if (replyCommentIds.isNotEmpty())
        {
            val replyQuery = KtQueryWrapper(ReplyEntity::class.java).select(ReplyEntity::id, ReplyEntity::content)
                .`in`(ReplyEntity::id, replyCommentIds)
            replyService.list(replyQuery).associateBy({ it.id }, { it.content })
        } else
        {
            emptyMap()
        }
        return replyList.map { reply ->
            val becauseContent = when
            {
                reply.replyCommentId != null -> replyCommentContentMap[reply.replyCommentId]
                reply.postCommentId != null -> postCommentContentMap[reply.postCommentId]
                reply.parCommentId != null -> parCommentContentMap[reply.parCommentId]
                else -> null
            }
            NoticeReplyResponse(
                id = reply.id, parCommentId = reply.parCommentId, userId = reply.userId, content = reply.content,
                likeCount = reply.likeCount, createdAt = reply.createdAt, postCommentId = reply.postCommentId,
                replyId = reply.replyCommentId, replyCommentId = reply.replyCommentId, because = becauseContent
            )
        }
    }


    @PostMapping
    fun createReply(@RequestBody request: CreateReplyRequest): Any
    {
        if (request.content.length > 255)
        {
            return ResultCode.PARAM_INVALID
        }
        if (request.parCommentId == null && request.postCommentId == null)
        {
            return ResultCode.MISSING_PARAM
        }
        val id = IdWorker.getId()
        val content = sensitiveWord.replace(request.content)
        val userId = StpUtil.getLoginIdAsLong()
        val replyEntity = ReplyEntity(
            id = id, userId = userId, content = content, replyId = request.replyId,
            postCommentId = request.postCommentId, parCommentId = request.parCommentId,
            replyCommentId = request.replyCommentId
        )

        val entityId: String = transactionTemplate.execute { status ->
            if (!changePostCommentCount.changePostCommentCount(request.postId, 1))
            {
                status.setRollbackOnly()
                return@execute null
            }
            if (request.parCommentId != null)
            {
                if (request.dataId == null)
                {
                    status.setRollbackOnly()
                    return@execute null
                }
                if (!parCommentService.addReplyCount(id = request.parCommentId, 1))
                {
                    status.setRollbackOnly()
                    return@execute null
                }
                paragraphService.incrementCount(
                    postId = request.postId, version = request.version, dataId = request.dataId
                )
            } else
            {
                postsCommentService.addReplyCount(id = request.postCommentId!!, 1)
            }
            replyService.save(replyEntity)
            redisTemplate.changeCommentCount(1, userId)
            //MQ TODO ，可以靠mq优化
            userService.createNotice(id, UserNoticeType.REPLY.type, request.replyId, targetUser = userId)
            return@execute id.toString()
        } ?: "error"
        val res = HashMap<String, String>()
        res["id"] = entityId
        res["content"] = content ?: ""
        return res
    }

    @GetMapping("/like")
    fun likeComment(@RequestParam id: Long, @RequestParam add: Boolean): Any
    {
        val key = "cursor:reply:like:$id"
        val increment = if (add) 1L else -1L
        redisTemplate.opsForValue().increment(key, increment)
        return ResultCode.SUCCESS
    }
}