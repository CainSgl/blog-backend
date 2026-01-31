package com.cainsgl.comment.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.toolkit.IdWorker
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.cainsgl.api.article.post.PostService
import com.cainsgl.api.article.util.ChangePostCommentCount
import com.cainsgl.comment.dto.request.CommentPostRequest
import com.cainsgl.comment.entity.PostsCommentEntity
import com.cainsgl.comment.service.PostsCommentServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.senstitve.config.SensitiveWord
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/post/comment")
class PostCommentController
{
    @Resource
     lateinit var changePostCommentCount: ChangePostCommentCount

    @Resource
    lateinit var postsCommentService: PostsCommentServiceImpl

    @Resource
    lateinit var transactionTemplate: TransactionTemplate

    //其他模块的调用
    @Resource
    lateinit var sensitiveWord: SensitiveWord

    @Resource
    lateinit var postService: PostService

    @Resource
    lateinit var redisTemplate: RedisTemplate<Any, Any>

    @GetMapping
    fun getByCursor(
        @RequestParam postId: Long, @RequestParam lastCreatedAt: LocalDateTime?,
        @RequestParam lastLikeCount: Int?,@RequestParam lastId: Long?
    ): List<PostsCommentEntity>
    {
        return postsCommentService.getByCursor(postId, lastCreatedAt, lastLikeCount,lastId)
    }
    @GetMapping("/locate")
    fun locate(id: Long):PostsCommentEntity?
    {
        //返回dataId
        val query= KtQueryWrapper(PostsCommentEntity::class.java).select(PostsCommentEntity::postId,
            PostsCommentEntity::version).eq(PostsCommentEntity::id, id)
        return postsCommentService.getOne(query)
    }
    @PostMapping
    fun create(@RequestBody request: CommentPostRequest): Any
    {
        val id = IdWorker.getId()
        //屏蔽敏感词
        val content=sensitiveWord.replace(request.content)
        return transactionTemplate.execute {
            postsCommentService.save(
                PostsCommentEntity(
                    id = id,
                    content = content,
                    userId = StpUtil.getLoginIdAsLong(),
                    version = request.version,
                    postId = request.postId
                )
            )
            changePostCommentCount.changePostCommentCount(request.postId, 1)

            val res=HashMap<String,String>()
            res["id"] = id.toString()
            res["content"]=content?:""
            return@execute res
        }?:ResultCode.UNKNOWN_ERROR
    }

    @GetMapping("/like")
    fun likeComment(@RequestParam id: Long,@RequestParam add: Boolean): Any
    {
        val key = "cursor:post_comment:like:$id"
        val increment = if (add) 1L else -1L
        redisTemplate.opsForValue().increment(key, increment)
        return ResultCode.SUCCESS
    }
}