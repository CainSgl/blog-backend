package com.cainsgl.article.controller


import cn.dev33.satoken.annotation.SaCheckPermission
import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.annotation.SaIgnore
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.cainsgl.api.ai.AiService
import com.cainsgl.api.user.extra.UserExtraInfoService
import com.cainsgl.article.dto.request.*
import com.cainsgl.article.dto.response.CreatePostResponse
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.article.service.PostChunkVectorServiceImpl
import com.cainsgl.article.service.PostHistoryServiceImpl
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.article.util.XssSanitizerUtils
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.DirectoryEntity
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.common.entity.article.PostHistoryEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import jakarta.validation.Valid
import org.apache.rocketmq.client.core.RocketMQClientTemplate
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime


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
    @SaCheckRole("user")
    @GetMapping("/last")
    fun getByLast(@RequestParam id: Long): Any
    {
        val post = postService.getPost(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        val userId = StpUtil.getLoginIdAsLong()
        val historyQuery = QueryWrapper<PostHistoryEntity>()
            .eq("post_id", post.id).eq("user_id",userId) .orderByDesc("version").last("LIMIT 1 OFFSET 1")
        val one = postHistoryService.getOne(historyQuery)
            ?: //无权限，直接返回
            return ResultCode.PERMISSION_DENIED
        //检查用户是否有权访问
        post.content=one.content
        return post
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
        val postEntity = PostEntity(
            id = request.id,
            title = request.title,
            summary = request.summary,
            status = ArticleStatus.DRAFT,
        )
        val historyQuery = QueryWrapper<PostHistoryEntity>()
            .eq("post_id", postEntity.id).eq("user_id",userId) .orderByDesc("version").last("LIMIT 1")
        val one = postHistoryService.getOne(historyQuery)
        if(one != null)
        {
            postHistoryService.updateById(PostHistoryEntity(id=one.id,content = entity.content,createdAt = LocalDateTime.now()))
        }else
        {
            postHistoryService.save(PostHistoryEntity(userId=userId,postId = entity.id, content = entity.content, createdAt = LocalDateTime.now(), version = 1))
        }
        //获取
        if (!postService.updateById(postEntity))
        {
            return ResultCode.PARAM_INVALID
        }
//        if (request.content != null)
//            rocketMQClientTemplate.asyncSendNormalMessage("article:content", request.id, null)
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
        //拿到最新历史版本
        val entity = postService.getOne(query)
            ?: //没对应的数据
            return ResultCode.RESOURCE_NOT_FOUND

        val historyQuery = QueryWrapper<PostHistoryEntity>()
            .eq("post_id", entity.id).eq("user_id",userId) .orderByDesc("version").last("LIMIT 1")
        val one = postHistoryService.getOne(historyQuery)
        //创建新的历史记录快照
        postHistoryService.save(PostHistoryEntity( userId=one.userId,postId=one.postId,version = one.version!!+1, createdAt = LocalDateTime.now(),content="咦，你是怎么看见我的？"))
        // 对文章内容进行XSS清理
        val sanitizedContent = XssSanitizerUtils.sanitize(one.content ?: "")
        entity.content=sanitizedContent
        //重新写回历史版本，防止有人查看历史版本被攻击
        one.content=sanitizedContent
        postHistoryService.updateById(one)

        val embedding = aiService.getEmbedding(entity.content!!)
        entity.vecotr=embedding
        entity.status=ArticleStatus.PUBLISHED
        postService.updateById(entity)
        //发送消息，这里不需要回调，也不需要保证可靠，不是强一致的需求
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

    @PostMapping("/history")
    fun history(@RequestBody @Valid request: HistoryPostRequest): List<PostHistoryEntity>
    {
        val historyQuery = QueryWrapper<PostHistoryEntity>()
            .eq("post_id", request.id).orderByDesc("version")
        return postHistoryService.list(historyQuery).apply { removeLast() }

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
