package com.cainsgl.article.controller

import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.cainsgl.api.user.follow.UserFollowService
import com.cainsgl.article.document.PostDocument
import com.cainsgl.article.dto.DirectoryTreeDTO
import com.cainsgl.article.dto.request.CreateKnowledgeBaseRequest
import com.cainsgl.article.dto.request.CursorKbRequest
import com.cainsgl.article.dto.request.PageUserIdListRequest
import com.cainsgl.article.dto.request.UpdateKnowledgeBaseRequest
import com.cainsgl.article.service.*
import com.cainsgl.common.dto.response.PageResponse
import com.cainsgl.common.dto.response.Result
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import com.cainsgl.common.entity.article.OperateType
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.common.exception.BusinessException
import com.cainsgl.senstitve.config.SensitiveWord
import jakarta.annotation.Resource
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.MediaType
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/post/kb")
class KnowledgeBaseController
{


    @Resource
    private lateinit var postDocumentService: PostDocumentService

    @Resource
    private lateinit var transactionTemplate: TransactionTemplate

    @Resource
    private lateinit var postService: PostServiceImpl

    @Resource
    private lateinit var userFollowService: UserFollowService

    @Resource
    lateinit var knowledgeBaseService: KnowledgeBaseServiceImpl

    @Resource
    lateinit var directoryService: DirectoryServiceImpl

    @Resource
    lateinit var sensitiveWord: SensitiveWord

    @Resource
    lateinit var postOperationService: PostOperationServiceImpl

    @Resource
    lateinit var postHistoryService: PostHistoryServiceImpl


    @GetMapping
    fun get(@RequestParam @Min(value = 1, message = "知识库id非法") id: Long): Any
    {
        val knowledgeBase: KnowledgeBaseEntity =
            knowledgeBaseService.getById(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        //还需要返回是否收藏没有

        if (knowledgeBase.status != ArticleStatus.PUBLISHED)
        {
            val userId = StpUtil.getLoginIdAsLong()
            if (knowledgeBase.status == ArticleStatus.ONLY_FANS)
            {
                if (userFollowService.hasFollow(userId, knowledgeBase.userId!!))
                {
                    val directoryTree: List<DirectoryTreeDTO> = directoryService.getDirectoryTreeByKbId(id)
                    return Triple(
                        knowledgeBase, directoryTree, postOperationService.hasOperate(userId, id, OperateType.STAR_KB)
                    )
                } else
                {
                    return Result.error(knowledgeBase.userId.toString())
//                    throw BusinessException(knowledgeBase.userId.toString())
                }
            }
            if (userId != knowledgeBase.userId)
            {
                return Result.error("由于私密性设置无法访问该知识库")
//                throw BusinessException()
            }
        }
        var stared = false
        if (StpUtil.isLogin())
        {
            val userId = StpUtil.getLoginIdAsLong()
            //尝试的去获取是否收藏
            stared = postOperationService.hasOperate(userId, id, OperateType.STAR_KB)
        }
        val directoryTree: List<DirectoryTreeDTO> = directoryService.getDirectoryTreeByKbId(id)
        return Triple(knowledgeBase, directoryTree, stared)
    }
    @GetMapping("/info")
    fun getBasicInfo(@RequestParam id: Long): Any
    {
        val entity= knowledgeBaseService.getById(id)?: return ResultCode.RESOURCE_NOT_FOUND
        if(entity.status == ArticleStatus.PUBLISHED)
        {
            return entity
        }
        val userId=StpUtil.getLoginIdAsLong()
        if(entity.userId==userId)
        {
            return entity
        }
        if (entity.status == ArticleStatus.ONLY_FANS)
        {
            return if (userFollowService.hasFollow(userId, entity.userId!!))
            {
                entity
            }else
            {
                ResultCode.SUCCESS
            }
        }
        return ResultCode.SUCCESS
    }
    @DeleteMapping
    fun delete(@RequestParam id: Long,@RequestParam(required = false) removePost: Boolean=false): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val query = KtQueryWrapper(KnowledgeBaseEntity::class.java).eq(KnowledgeBaseEntity::id, id)
            .eq(KnowledgeBaseEntity::userId, userId)
        return transactionTemplate.execute {
            if (knowledgeBaseService.remove(query))
            {
                if(removePost)
                {
                    val postQuery = KtQueryWrapper(PostEntity::class.java).select(PostEntity::id, PostEntity::status).eq(PostEntity::kbId, id)
                    val list = postService.list(postQuery)
                    list.forEach {
                        if(it.status==ArticleStatus.ONLY_FANS||it.status==ArticleStatus.PUBLISHED)
                        {
                            postDocumentService.delete(it.id!!)
                        }
                    }
                    postService.removeByIds(list)
                    return@execute list.size
                }

                val postQuery =
                    KtUpdateWrapper(PostEntity::class.java).eq(PostEntity::kbId, id).set(PostEntity::kbId, null)
                        .set(PostEntity::status, ArticleStatus.NO_KB)
                //影响了这么多个文章
                return@execute postService.baseMapper.update(postQuery)
            } else
            {
                return@execute ResultCode.RESOURCE_NOT_FOUND
            }
        } ?: ResultCode.UNKNOWN_ERROR
    }


    @GetMapping("/basic")
    fun getBasic(@RequestParam @Min(value = 1, message = "知识库id非法") id: Long): Any
    {
        val knowledgeBase: KnowledgeBaseEntity =
            knowledgeBaseService.getById(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        if (knowledgeBase.status != ArticleStatus.PUBLISHED)
        {
            if (knowledgeBase.status == ArticleStatus.ONLY_FANS)
            {
                if (!userFollowService.hasFollow(StpUtil.getLoginIdAsLong(), knowledgeBase.userId!!))
                {
                    throw BusinessException(knowledgeBase.userId.toString())
                }
            }
            val userId = StpUtil.getLoginIdAsLong()
            if (userId != knowledgeBase.userId)
            {
                throw BusinessException("由于私密性设置无法访问该知识库")
            }
        }
        return knowledgeBase
    }


    @PostMapping("/list")
    fun list(@RequestBody @Valid request: PageUserIdListRequest): PageResponse<KnowledgeBaseEntity>
    {
        val pageParam = Page<KnowledgeBaseEntity>(request.page, request.size).apply {
            if (request.simple)
            {
                setSearchCount(false)
            }
        }
        val queryWrapper = QueryWrapper<KnowledgeBaseEntity>().apply {
            eq("user_id", request.userId)
            if (StpUtil.isLogin())
            {
                val userId = StpUtil.getLoginIdAsLong()
                if (userId == request.userId && request.status != null)
                {
                    eq("status", request.status)
                } else if (userId != request.userId)
                {
                    //登录，但是不是本人
                    eq("status", ArticleStatus.PUBLISHED).or().eq("status", ArticleStatus.ONLY_FANS)
                }
            } else
            {
                eq("status", ArticleStatus.PUBLISHED).or().eq("status", ArticleStatus.ONLY_FANS)
            }
            if (!request.option.isNullOrEmpty())
            {
                if (PageUserIdListRequest.kbOptions.contains(request.option))
                {
                    //可以作为orderBy
                    orderByDesc(request.option)
                }
            }
            if (!request.keyword.isNullOrEmpty())
            {
                like("name", request.keyword.lowercase())
            }
            if (request.onlyTitle)
            {
                select("name")
            }
        }
        val result = knowledgeBaseService.page(pageParam, queryWrapper)
        return PageResponse(
            records = result.records, total = result.total, pages = result.pages, current = result.current,
            size = result.size
        )
    }


    @GetMapping("/index")
    fun getIndex(@RequestParam @Min(value = 1, message = "知识库id非法") id: Long): Any
    {
        val query =
            KtQueryWrapper(KnowledgeBaseEntity::class.java).select(KnowledgeBaseEntity::index, KnowledgeBaseEntity::id)
                .eq(KnowledgeBaseEntity::id, id)
        return knowledgeBaseService.getOne(query) ?: ResultCode.RESOURCE_NOT_FOUND
    }


    @PostMapping
    fun createKnowledgeBase(@RequestBody @Valid request: CreateKnowledgeBaseRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val kbEntity = KnowledgeBaseEntity(
            userId = userId, name = sensitiveWord.replace(request.name), index = request.index,
            coverUrl = request.coverUrl
        )
        if (knowledgeBaseService.save(kbEntity))
        {
            return kbEntity
        }
        return ResultCode.DB_ERROR
    }


    @PutMapping
    fun updateKnowledgeBase(@RequestBody @Valid request: UpdateKnowledgeBaseRequest): ResultCode
    {
        val userId = StpUtil.getLoginIdAsLong()
        val updateWrapper = KtUpdateWrapper(KnowledgeBaseEntity::class.java).eq(KnowledgeBaseEntity::id, request.id)
            .eq(KnowledgeBaseEntity::userId, userId)
        val kbEntity = KnowledgeBaseEntity(
            status = request.status, name = sensitiveWord.replace(request.name),
            index = sensitiveWord.replace(request.content), coverUrl = request.coverUrl
        )
        if (knowledgeBaseService.update(kbEntity, updateWrapper))
        {
            return ResultCode.SUCCESS
        }
        return ResultCode.DB_ERROR
    }


    @SaCheckRole("admin")
    @GetMapping("/changeLikeCount")
    fun changeLikeCount(@RequestParam count: Int, @RequestParam kbId: Long)
    {
        return knowledgeBaseService.addKbLikeCount(kbId = kbId, count)
    }

    @PostMapping("/cursor")
    fun cursor(@RequestBody request: CursorKbRequest): Any
    {
        return knowledgeBaseService.cursor(request.lastCreatedAt, request.lastLike, request.lastId, request.pageSize)
    }

    /**
     * 流式返回公开知识库下所有未公开的文档
     * @param kbId 知识库ID
     * @return SSE流式响应
     */
    @GetMapping("/publish-all-posts", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun publishAllPosts(@RequestParam @Min(value = 1, message = "知识库id非法") kbId: Long): SseEmitter
    {
        val userId = StpUtil.getLoginIdAsLong()
        val emitter = SseEmitter(300000L) // 5分钟超时
        
        // 验证知识库所有权
        val kb = knowledgeBaseService.getById(kbId)
        if (kb == null || kb.userId != userId)
        {
            emitter.completeWithError(BusinessException("知识库不存在或无权限操作"))
            return emitter
        }

        CompletableFuture.runAsync {
            try
            {
                // 查询该知识库下所有未公开的文档（需要完整信息）
                val query = KtQueryWrapper(PostEntity::class.java)
                    .eq(PostEntity::kbId, kbId)
                    .eq(PostEntity::userId, userId)
                    .`in`(PostEntity::status, listOf(
                        ArticleStatus.DRAFT,
                        ArticleStatus.PENDING_REVIEW,
                        ArticleStatus.OFF_SHELF,
                        ArticleStatus.NO_KB
                    ))

                val unpublishedPosts = postService.list(query)
                
                if (unpublishedPosts.isEmpty())
                {
                    emitter.send(SseEmitter.event()
                        .name("info")
                        .data(mapOf("message" to "没有需要公开的文档")))
                    emitter.complete()
                    return@runAsync
                }

                emitter.send(SseEmitter.event()
                    .name("start")
                    .data(mapOf("total" to unpublishedPosts.size)))

                var successCount = 0
                var failCount = 0

                // 批量发布文档，参考PostController.publish的逻辑
                unpublishedPosts.forEach { post ->
                    try
                    {
                        // 获取编辑文档的最新版本
                        val historyQuery = KtQueryWrapper(com.cainsgl.common.entity.article.PostHistoryEntity::class.java)
                            .select(
                                com.cainsgl.common.entity.article.PostHistoryEntity::id,
                                com.cainsgl.common.entity.article.PostHistoryEntity::content,
                                com.cainsgl.common.entity.article.PostHistoryEntity::version,
                                com.cainsgl.common.entity.article.PostHistoryEntity::userId
                            )
                            .eq(com.cainsgl.common.entity.article.PostHistoryEntity::postId, post.id)
                            .eq(com.cainsgl.common.entity.article.PostHistoryEntity::userId, userId)
                            .orderByDesc(com.cainsgl.common.entity.article.PostHistoryEntity::version)
                            .last("LIMIT 1")
                        
                        val history = postHistoryService.getOne(historyQuery)
                        
                        if (history == null || history.content.isNullOrEmpty())
                        {
                            failCount++
                            emitter.send(SseEmitter.event()
                                .name("progress")
                                .data(mapOf(
                                    "postId" to post.id,
                                    "title" to post.title,
                                    "status" to "failed",
                                    "message" to "没有历史版本或内容为空",
                                    "processed" to (successCount + failCount),
                                    "total" to unpublishedPosts.size
                                )))
                            return@forEach
                        }

                        // 内容清理：XSS过滤和敏感词替换
                        val sanitizedContent = sensitiveWord.replace(
                            com.cainsgl.article.util.XssSanitizerUtils.sanitize(history.content!!)!!
                        )!!

                        // 检查内容是否有变更
                        if (post.content == sanitizedContent)
                        {
                            // 内容没变，只更新状态
                            val updateWrapper = KtUpdateWrapper(PostEntity::class.java)
                                .eq(PostEntity::id, post.id)
                                .eq(PostEntity::userId, userId)
                                .set(PostEntity::status, ArticleStatus.PUBLISHED)
                                .set(PostEntity::publishedAt, LocalDateTime.now())
                            
                            postService.update(updateWrapper)
                            successCount++
                            
                            emitter.send(SseEmitter.event()
                                .name("progress")
                                .data(mapOf(
                                    "postId" to post.id,
                                    "title" to post.title,
                                    "status" to "success",
                                    "processed" to (successCount + failCount),
                                    "total" to unpublishedPosts.size
                                )))
                            return@forEach
                        }

                        // 更新post内容和状态
                        post.content = sanitizedContent
                        post.version = history.version
                        post.status = ArticleStatus.PUBLISHED
                        post.publishedAt = LocalDateTime.now()
                        
                        // 更新历史版本内容（防止XSS攻击）
                        history.content = sanitizedContent
                        history.createdAt = LocalDateTime.now()
                        postHistoryService.updateById(history)
                        
                        // 创建新的历史版本供作者继续编辑
                        postHistoryService.save(
                            com.cainsgl.common.entity.article.PostHistoryEntity(
                                userId = userId,
                                postId = post.id,
                                version = history.version!! + 1,
                                createdAt = LocalDateTime.now(),
                                content = sanitizedContent
                            )
                        )
                        
                        // 更新post实体
                        postService.updateById(post)
                        
                        // 同步到Elasticsearch
                        if (sanitizedContent.isNotEmpty())
                        {
                            postDocumentService.save(
                                PostDocument(
                                    id = post.id!!,
                                    title = post.title ?: "",
                                    summary = post.summary,
                                    img = post.img,
                                    content = sanitizedContent,
                                    tags = post.tags
                                )
                            )
                        }
                        
                        // 清除缓存
                        postService.removeCache(post.id!!)
                        
                        successCount++
                        emitter.send(SseEmitter.event()
                            .name("progress")
                            .data(mapOf(
                                "postId" to post.id,
                                "title" to post.title,
                                "status" to "success",
                                "processed" to (successCount + failCount),
                                "total" to unpublishedPosts.size
                            )))
                    }
                    catch (e: Exception)
                    {
                        failCount++
                        emitter.send(SseEmitter.event()
                            .name("error")
                            .data(mapOf(
                                "postId" to post.id,
                                "title" to post.title,
                                "message" to (e.message ?: "未知错误")
                            )))
                    }
                }

                // 发送完成事件
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(mapOf(
                        "total" to unpublishedPosts.size,
                        "success" to successCount,
                        "failed" to failCount
                    )))
                
                emitter.complete()
            }
            catch (e: Exception)
            {
                emitter.completeWithError(e)
            }
        }

        return emitter
    }
}
