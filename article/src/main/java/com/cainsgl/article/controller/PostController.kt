package com.cainsgl.article.controller


import cn.dev33.satoken.annotation.SaCheckPermission
import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.annotation.SaIgnore
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.cainsgl.api.ai.AiService
import com.cainsgl.api.user.extra.UserExtraInfoService
import com.cainsgl.article.dto.request.CreatePostRequest
import com.cainsgl.article.dto.request.SearchPostRequest
import com.cainsgl.article.dto.request.UpdatePostRequest
import com.cainsgl.article.dto.response.CreatePostResponse
import com.cainsgl.article.dto.response.GetPostResponse
import com.cainsgl.article.service.*
import com.cainsgl.article.util.XssSanitizerUtils
import com.cainsgl.common.dto.request.OnlyId
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

/**
 * 这里的逻辑是这样的，其他用户只能访问对应文章的历史版本，或者发布版本
 * 历史版本的产生只有作者公布才会产生，发布后，文章的发布版本为历史版本的最新版，并且生成一个新的历史版本提供给当前作者继续使用和保存
 * 每次修改都是修改的历史版本的最新版本（其他人不可见）
 * 也就是一个文档会被至少存档4份，一份是post的content（这里比较冗余是版本遗留问题），一份是历史分支的两份（最新一份是方便作者在原来的基础上开发！获取的是最新版本），以及原来发布的那份，可以作为历史版本回溯，一份是postClone表，也是历史版本的遗留导致的，他是为了验证用户是否更新了版本来重新登录向量，预计在最新版本移除
 */
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
    @Resource
    lateinit var postOperationService: PostOperationServiceImpl
    //来自其他模块的，只能通过Service来访问
    @Resource
    lateinit var userExtraInfoService:UserExtraInfoService
    @Resource
    lateinit var aiService:AiService
    @SaIgnore
    @GetMapping
    fun get(@RequestParam id: Long,@RequestParam simple: Boolean=false): Any
    {
        val post = postService.getPost(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        //检查用户是否有权访问
        if (post.status == ArticleStatus.PUBLISHED)
        {
            if(simple)
            {
                return post
            }
            //获取自己是否点赞的信息，并添加上
            if (StpUtil.isLogin())
            {
                val userId = StpUtil.getLoginIdAsLong()
                val operate= postOperationService.getOperateByUserIdAndPostId(userId=userId,postId=post.id!!)
                return GetPostResponse(post,operate)
            }
            return GetPostResponse(post, emptySet())
        }
        //限制为只允许该用户访问
        try
        {
            val userId = StpUtil.getLoginIdAsLong()
            if (userId == post.userId)
            {
                if(simple)
                {
                    return post
                }
                val operate= postOperationService.getOperateByUserIdAndPostId(userId=userId,postId=post.id!!)
                return GetPostResponse(post,operate)
            }
        } catch (e: Exception)
        {
            log.info { "未登录的请求访问私密文章$e" }
            return ResultCode.USER_NOT_LOGIN
        }
        return ResultCode.PERMISSION_DENIED
    }

    @GetMapping("/top")
    fun getTopPostByUserId(@RequestParam id: Long): Any
    {
        val query= QueryWrapper<PostEntity>()
        query.select(PostEntity.BASIC_COL)
        query.eq("user_id", id)
        query.eq("is_top",true)
        query.eq("status",ArticleStatus.PUBLISHED)
        query.orderByDesc("published_at")
        return postService.list(query)
    }

    @SaCheckRole("user")
    @GetMapping("/last")
    fun getByLast(@RequestParam id: Long): Any
    {
        val post = postService.getPost(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        val userId = StpUtil.getLoginIdAsLong()
        if(post.userId != userId)
        {
            //不是他的，这个是历史版本的缓存版本
            return ResultCode.PERMISSION_DENIED
        }
        val historyQuery = QueryWrapper<PostHistoryEntity>()
            .select("content")
            .eq("post_id", post.id).eq("user_id",userId) .orderByDesc("version").last("LIMIT 1")
        val one = postHistoryService.getOne(historyQuery)
        //检查用户是否有权访问
        post.content=one?.content
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
            img=request.img
        )
        val historyQuery = QueryWrapper<PostHistoryEntity>()
            .eq("post_id", postEntity.id).eq("user_id",userId) .orderByDesc("version").last("LIMIT 1")
        val one = postHistoryService.getOne(historyQuery)
        if(one != null)
        {
            postHistoryService.updateById(PostHistoryEntity(id=one.id,content = request.content,createdAt = LocalDateTime.now()))
        }else
        {
            //这里是为了防止没有最新版本供作者使用
            postHistoryService.save(PostHistoryEntity(userId=userId,postId = entity.id, content = request.content, createdAt = LocalDateTime.now(), version = 1))
        }
        //获取
        if(postEntity.needUpdate())
          postService.updateById(postEntity)
        return ResultCode.SUCCESS
    }

    @SaCheckRole("user")
    @PostMapping("/publish")
    fun publish(@RequestBody @Valid request: OnlyId): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        //这里是读时更新Read-Modify-Write，正常是要加事务的，但是这里是单个用户，不存在并发问题
        val query = QueryWrapper<PostEntity>()
        query.eq("id", request.id)
        query.eq("user_id", userId)
        //拿到最新历史版本
        val entity = postService.getOne(query)
            ?: //没对应的数据
            return ResultCode.RESOURCE_NOT_FOUND
        //获取编辑文档的最新版本，用来发布
        val historyQuery = QueryWrapper<PostHistoryEntity>()
            .select("id","content","version")
            .eq("post_id", entity.id).eq("user_id",userId) .orderByDesc("version").last("LIMIT 1")
        val one = postHistoryService.getOne(historyQuery)
        val sanitizedContent = XssSanitizerUtils.sanitize(one.content!!)
        //检验是否有内容变更
        if(entity.content==sanitizedContent)
        {
            //完全是之前的版本，什么都不用做
            return ResultCode.SUCCESS
        }
        entity.content=sanitizedContent
        entity.version=one.version
        //重新写回历史版本，防止有人查看历史版本被攻击
        one.content=sanitizedContent
        //剩下的异步去处理就好了
        Thread.ofVirtual().start{
            postHistoryService.updateById(one)
            //注：这里再发布一个最新版本，是为了作者下次编辑文档的时候，返回他就好了
            postHistoryService.save(PostHistoryEntity( userId=one.userId,postId=entity.id,version = one.version!!+1, createdAt = LocalDateTime.now(),content=sanitizedContent))
            if(entity.content!!.isEmpty())
            {
                return@start
            }
            val embedding = aiService.getEmbedding(entity.content!!)
            entity.vecotr=embedding
            entity.status=ArticleStatus.PUBLISHED
            postService.updateById(entity)
            //发送消息，这里不需要回调，也不需要保证可靠，不是强一致的需求，毕竟只是一次版本的迭代，问题不大
            rocketMQClientTemplate.asyncSendNormalMessage("article:publish", request.id, null)
            //延时双删
            //删除缓存
            postService.removeCache(entity.id!!)
            Thread.sleep(500)
            postService.removeCache(entity.id!!)
        }
        return ResultCode.SUCCESS
    }

    @SaCheckPermission("article.delete")
    @DeleteMapping
    fun deletePost(@RequestParam id: Long):Any
    {
        //还需要删除目录
        val userId = StpUtil.getLoginIdAsLong()
        val wrapper= UpdateWrapper<PostEntity>()
        wrapper.eq("id",id)
        wrapper.eq("user_id", userId)
        wrapper.set("status", ArticleStatus.OFF_SHELF)
        return transactionTemplate.execute {
            if (!postService.update(wrapper))
            {
                return@execute ResultCode.RESOURCE_NOT_FOUND
            }
            val wrapper2= QueryWrapper<DirectoryEntity>()
            wrapper2.eq("post_id",id)
            directoryService.remove(wrapper2)
        }?:ResultCode.SUCCESS

        //删除历史版本

    //    rocketMQClientTemplate.asyncSendNormalMessage("article:delete", id, null)
    }

    @PostMapping("/history")
    fun history(@RequestBody @Valid request: OnlyId): List<PostHistoryEntity>
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
