package com.cainsgl.comment.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.toolkit.IdWorker
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.cainsgl.api.article.post.PostService
import com.cainsgl.comment.dto.request.CreateParagraphRequest
import com.cainsgl.comment.entity.ParCommentEntity
import com.cainsgl.comment.service.ParCommentServiceImpl
import com.cainsgl.comment.service.ParagraphServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.util.user.UserHotInfoUtils.Companion.changeCommentCount
import com.cainsgl.senstitve.config.SensitiveWord
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/comment")
class ParCommentController
{
    @Resource
    private lateinit var parCommentService: ParCommentServiceImpl

    @Resource
    lateinit var paragraphService: ParagraphServiceImpl

    @Resource
    lateinit var transactionTemplate: TransactionTemplate

    @Resource
    lateinit var sensitiveWord: SensitiveWord
    //其他模块的
    @Resource
    lateinit var postService: PostService
    @Resource
    lateinit var redisTemplate: RedisTemplate<Any, Any>

    //创建评论
    @PostMapping
    fun createComment(@RequestBody request: CreateParagraphRequest): Any
    {
        if (request.content.length > 255)
        {
            return ResultCode.PARAM_INVALID
        }
        return transactionTemplate.execute { status ->
            val id = IdWorker.getId()
            if (!postService.addCommentCount(id = request.postId, 1))
            {
                status.setRollbackOnly()
                return@execute null
            }
            val content = sensitiveWord.replace(request.content)
            paragraphService.incrementCount(postId = request.postId, version = request.version, dataId = request.dataId)
            val userId = StpUtil.getLoginIdAsLong()
            parCommentService.save(
                ParCommentEntity(
                    id = id, userId = userId, dataId = request.dataId, version = request.version,
                    postId = request.postId, content = content
                )
            )
            redisTemplate.changeCommentCount(1, userId)
            val res = HashMap<String, String>()
            res["id"] = id.toString()
            res["content"] = content ?: ""
            return@execute res
        } ?: "error"
    }

    //游标获取评论
    @GetMapping
    fun getComment(
        @RequestParam postId: Long, @RequestParam version: Int, @RequestParam dataId: Int,
        @RequestParam lastCreatedAt: LocalDateTime?, @RequestParam lastLikeCount: Int?, @RequestParam lastId: Long?
    ): List<ParCommentEntity>
    {
        return parCommentService.getByCursor(postId, version, dataId, lastCreatedAt, lastLikeCount, lastId)
    }
    @GetMapping("/locate")
    fun locate(id: Long):ParCommentEntity
    {
        //返回dataId
        val query= KtQueryWrapper(ParCommentEntity::class.java).select(ParCommentEntity::dataId, ParCommentEntity::postId,
            ParCommentEntity::version).eq(ParCommentEntity::id, id)
       return parCommentService.getOne(query)
    }
    @GetMapping("/notice")
    fun getCommentForNotice(@RequestParam id: Long):Any?
    {
        val query = KtQueryWrapper(ParCommentEntity::class.java).eq(ParCommentEntity::id, id)
        return parCommentService.getOne(query)
    }
}