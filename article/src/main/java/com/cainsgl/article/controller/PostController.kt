package com.cainsgl.article.controller


import cn.dev33.satoken.annotation.SaCheckPermission
import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.cainsgl.api.ai.AiService
import com.cainsgl.api.user.extra.UserExtraInfoService
import com.cainsgl.article.dto.request.post.CreatePostRequest
import com.cainsgl.article.dto.request.post.PubPostRequest
import com.cainsgl.article.dto.request.post.SearchPostRequest
import com.cainsgl.article.dto.request.post.UpdatePostRequest
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.article.service.PostChunkVectorServiceImpl
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.DirectoryEntity
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.common.exception.BusinessException
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

    @Resource
    lateinit var postChunkVectorService: PostChunkVectorServiceImpl

    //来自其他模块的，只能通过Service来访问
    @Resource
    lateinit var userExtraInfoService:UserExtraInfoService
    @Resource
    lateinit var aiService:AiService
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
            //发送消息
            rocketMQClientTemplate.asyncSendNormalMessage("article:post", postEntity.id, null)
            return@execute postEntity
        } ?: ResultCode.UNKNOWN_ERROR

    }


    @SaCheckRole("user")
    @PutMapping
    fun updatePost(@RequestBody request: UpdatePostRequest): Any
    {
        requireNotNull(request.id) { return ResultCode.MISSING_PARAM }
        val userId = StpUtil.getLoginIdAsLong()
        val updateWrapper = UpdateWrapper<PostEntity>()
        updateWrapper.eq("id", request.id)
        updateWrapper.eq("user_id", userId)
        val postEntity = PostEntity(
            id = request.id,
            title = request.title,
            content = request.content,
            summary = request.summary,
            top = request.isTop
        )
        if (!postService.update(postEntity, updateWrapper))
        {
            return ResultCode.PARAM_INVALID
        }
        //发送消息，这里不需要回调，也不需要保证可靠，不是强一致的需求
        if (request.content != null)
            rocketMQClientTemplate.asyncSendNormalMessage("article:content", request.id, null)
        return ResultCode.SUCCESS
    }

    @SaCheckRole("user")
    @PutMapping("/publish")
    fun updatePost(@RequestBody request: PubPostRequest): Any
    {
        requireNotNull(request.id) { return ResultCode.MISSING_PARAM }
        val userId = StpUtil.getLoginIdAsLong()
        val updateWrapper = UpdateWrapper<PostEntity>()
        updateWrapper.eq("id", request.id)
        updateWrapper.eq("user_id", userId)
        updateWrapper.eq("status", ArticleStatus.DRAFT)
        val postEntity = PostEntity(
            id = request.id,
            status = ArticleStatus.PUBLISHED
        )
        if (!postService.update(postEntity, updateWrapper))
        {
            throw BusinessException("数据库无法更新该数据，可能是资源不存在或者传参问题")
        }
        //强一致需求，必须入库本地消息表，如果失败的话
        //使用grpc传入
        rocketMQClientTemplate.asyncSendNormalMessage("article:publish", request.id, null)
        return ResultCode.SUCCESS
    }

    @SaCheckPermission("article.delete")
    @DeleteMapping
    fun deletePost(@RequestParam id: Long):Any
    {
        //还需要删除目录
        val userId = StpUtil.getLoginIdAsLong()

        val wrapper= QueryWrapper<PostEntity>()
        wrapper.eq("id",id)
        wrapper.eq("user_id", userId)
        if (!postService.remove(wrapper))
        {
            return ResultCode.RESOURCE_NOT_FOUND
        }
        val wrapper2= QueryWrapper<DirectoryEntity>()
        wrapper2.eq("post_id",id)
        directoryService.remove(wrapper2)
        rocketMQClientTemplate.asyncSendNormalMessage("article:delete", id, null)
        return ResultCode.SUCCESS
    }

    @SaCheckRole("user")
    @PostMapping("/search")
    fun searchPost(@RequestBody request: SearchPostRequest):Any
    {
        require(!request.query.isNullOrEmpty())
        if (request.vectorOffset==null)
        {
            request.vectorOffset=1.1
        }
        //向量化，并且尝试加上用户的兴趣度偏向
        var userOffsetVector:FloatArray?= null
        try{
             val userId= StpUtil.getLoginIdAsLong()
            userOffsetVector= userExtraInfoService.getInterestVector(userId)
            //从用户额外信息表里获取
        }catch (e:Exception){
            //没有，不管
        }
        var embedding = aiService.getEmbedding(request.query!!)
        if(userOffsetVector!=null)
        {
            embedding += userOffsetVector
        }
        return postChunkVectorService.getPostsByVector(targetVector = embedding, request.vectorOffset!!)
    }

}
