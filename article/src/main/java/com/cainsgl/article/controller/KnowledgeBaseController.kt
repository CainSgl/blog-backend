package com.cainsgl.article.controller

import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.cainsgl.api.user.follow.UserFollowService
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
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*

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
}
