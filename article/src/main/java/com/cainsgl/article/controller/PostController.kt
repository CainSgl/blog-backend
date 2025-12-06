package com.cainsgl.article.controller


import cn.dev33.satoken.annotation.SaCheckPermission
import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.article.dto.request.post.CreatePostRequest
import com.cainsgl.article.dto.request.post.UpdatePostRequest
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.PostEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.apache.rocketmq.client.core.RocketMQClientTemplate
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*


private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/post")
class PostController
{

    @Resource
    lateinit var postService: PostServiceImpl

    @Resource
    lateinit var directoryService: DirectoryServiceImpl

    @Resource
    lateinit var rocketMQClientTemplate: RocketMQClientTemplate

    @Resource
    lateinit var transactionTemplate: TransactionTemplate

    /**
     * 根据ID获取文章
     */
    @GetMapping
    fun get(@RequestParam(required = false) id: Long?): Any
    {
        requireNotNull(id) { return ResultCode.MISSING_PARAM }
        val post = postService.getPost(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        //检查用户是否有权访问
        if (post.status == ArticleStatus.PUBLISHED)
        {
            return post
        }
        //限制为只允许该用户访问
        try
        {
            val userId = StpUtil.getLoginIdAsLong()
            if (userId == post.userId)
            {
                return post
            }
        } catch (e: Exception)
        {
            log.info { "未登录的请求访问私密文章$e" }
            return ResultCode.USER_NOT_LOGIN
        }
        return ResultCode.PERMISSION_DENIED
    }

    @SaCheckPermission("article.post")
    @PostMapping
    fun createPost(@RequestBody request: CreatePostRequest): Any
    {
        requireNotNull(request.kbId) { return ResultCode.MISSING_PARAM }
        requireNotNull(request.parentId) { return ResultCode.MISSING_PARAM }
        require(request.kbId >= 0 && request.parentId >= 0) { return ResultCode.PARAM_INVALID }
        val userId = StpUtil.getLoginIdAsLong()
        //创建一个目录，然后让他的postId=新建的文档ID
        val postEntity = PostEntity(title = request.title, userId = userId, kbId = request.kbId)
        //开启事务
        return transactionTemplate.execute { status ->
            //事务内执行
            if (!postService.save(postEntity))
            {
                status.setRollbackOnly()
                return@execute ResultCode.DB_ERROR
            }
            if (!directoryService.saveDirectory(request.kbId, userId = userId, request.title, request.parentId, postEntity.id))
            {
                status.setRollbackOnly()
                //多半是参数问题
                return@execute ResultCode.PARAM_INVALID
            }
            return@execute ResultCode.SUCCESS
        } ?: ResultCode.UNKNOWN_ERROR

    }


    @SaCheckRole("user")
    @PutMapping
    fun updatePost(@RequestBody request: UpdatePostRequest): Any
    {
        //TODO 更新文档，注意状态变更，发送不同的消息
        return ResultCode.SUCCESS
    }

}
