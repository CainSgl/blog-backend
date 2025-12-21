package com.cainsgl.article.controller


import cn.dev33.satoken.annotation.SaCheckPermission
import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.annotation.SaIgnore
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.cainsgl.api.ai.AiService
import com.cainsgl.api.user.extra.UserExtraInfoService
import com.cainsgl.article.dto.request.CreatePostRequest
import com.cainsgl.article.dto.request.PubPostRequest
import com.cainsgl.article.dto.request.SearchPostRequest
import com.cainsgl.article.dto.request.UpdatePostRequest
import com.cainsgl.article.dto.response.CreatePostResponse
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.article.service.PostChunkVectorServiceImpl
import com.cainsgl.article.service.PostHistoryServiceImpl
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.DirectoryEntity
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.common.entity.article.PostHistoryEntity
import com.cainsgl.common.exception.BusinessException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import jakarta.validation.Valid
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

    @Resource
    lateinit var postHistoryService: PostHistoryServiceImpl

    //来自其他模块的，只能通过Service来访问
    @Resource
    lateinit var userExtraInfoService:UserExtraInfoService
    @Resource
    lateinit var aiService:AiService
    @SaIgnore
    @GetMapping
    fun get(@RequestParam id: Long): Any
    {
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
    fun createPost(@RequestBody @Valid request: CreatePostRequest): Any
    {
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
            val dirId= directoryService.saveDirectory(request.kbId, userId = userId, request.title, request.parentId, postEntity.id)
            if (dirId<0)
            {
                status.setRollbackOnly()
                //多半是参数问题
                return@execute ResultCode.PARAM_INVALID
            }
            //发送消息
            rocketMQClientTemplate.asyncSendNormalMessage("article:post", postEntity.id, null)
            return@execute CreatePostResponse(postEntity,dirId)
        } ?: ResultCode.UNKNOWN_ERROR
    }


    @SaCheckRole("user")
    @PutMapping
    fun updatePost(@RequestBody @Valid request: UpdatePostRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val query = QueryWrapper<PostEntity>()
        query.eq("id", request.id)
        query.eq("user_id", userId)
        val entity = postService.getOne(query)
            ?: //没对应的数据
            return ResultCode.RESOURCE_NOT_FOUND
//        val updateWrapper = UpdateWrapper<PostEntity>()
//        updateWrapper.eq("id", request.id)
//        updateWrapper.eq("user_id", userId)
        val postEntity = PostEntity(
            id = request.id,
            title = request.title,
            content = request.content,
            summary = request.summary,
            top = request.isTop,
            status = ArticleStatus.DRAFT,
        )
        if(!request.auto)
        {
         //说明是用户手动保存的，记录到历史版本去
            postHistoryService.save(PostHistoryEntity(userId=userId,postId = entity.id, content = entity.content,))
        }
        //不再需要，后面重新发布一次
//        if(entity.status == ArticleStatus.PUBLISHED && !request.content.isNullOrEmpty())
//        {
//            //修改的是发布状态的内容。需要重新向量化
//            val embedding = aiService.getEmbedding(entity.content!!)
//            postEntity.vecotr=embedding
//            rocketMQClientTemplate.asyncSendNormalMessage("article:publish", request.id, null)
//        }
        //获取
        if (!postService.updateById(postEntity))
        {
            return ResultCode.PARAM_INVALID
        }
        //发送消息，这里不需要回调，也不需要保证可靠，不是强一致的需求
        if (request.content != null)
            rocketMQClientTemplate.asyncSendNormalMessage("article:content", request.id, null)
        return ResultCode.SUCCESS
    }

    @SaCheckRole("user")
    @PostMapping("/publish")
    fun publish(@RequestBody @Valid request: PubPostRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        //这里是读时更新Read-Modify-Write，正常是要加事务的，但是这里是单个用户，不存在并发问题
        val query = QueryWrapper<PostEntity>()
        query.eq("id", request.id)
        query.eq("user_id", userId)
        query.eq("status", ArticleStatus.DRAFT)
        val entity = postService.getOne(query)
            ?: //没对应的数据
            return ResultCode.RESOURCE_NOT_FOUND
        val embedding = aiService.getEmbedding(entity.content!!)
        entity.vecotr=embedding
        entity.status=ArticleStatus.PUBLISHED
        if (!postService.updateById(entity))
        {
            throw BusinessException("数据库无法更新该数据，可能是资源不存在或者传参问题")
        }
        //发送消息，这里不需要回调，也不需要保证可靠，不是强一致的需求
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

    @SaIgnore
    @PostMapping("/search")
    fun searchPost(@RequestBody request: SearchPostRequest):Any
    {
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
        var embedding = aiService.getEmbedding(request.query)
        if(userOffsetVector!=null)
        {
            embedding += userOffsetVector
        }
        return postChunkVectorService.getPostsByVector(targetVector = embedding, request.vectorOffset!!)
    }

}
