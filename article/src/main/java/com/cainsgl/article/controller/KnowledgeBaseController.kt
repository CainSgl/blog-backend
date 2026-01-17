package com.cainsgl.article.controller

import cn.dev33.satoken.annotation.SaCheckPermission
import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.annotation.SaIgnore
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.cainsgl.api.user.follow.UserFollowService
import com.cainsgl.article.dto.DirectoryTreeDTO
import com.cainsgl.article.dto.request.CreateKnowledgeBaseRequest
import com.cainsgl.article.dto.request.CursorKbRequest
import com.cainsgl.article.dto.request.PageUserIdListRequest
import com.cainsgl.article.dto.request.UpdateKnowledgeBaseRequest
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.article.service.KnowledgeBaseServiceImpl
import com.cainsgl.common.dto.response.PageResponse
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import com.cainsgl.common.exception.BusinessException
import com.cainsgl.senstitve.config.SensitiveWord
import jakarta.annotation.Resource
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/kb")
class KnowledgeBaseController
{


    @Autowired
    private lateinit var userFollowService: UserFollowService

    @Resource
    lateinit var knowledgeBaseService: KnowledgeBaseServiceImpl

    @Resource
    lateinit var directoryService: DirectoryServiceImpl
    @Resource
    lateinit var sensitiveWord: SensitiveWord
    @SaIgnore
    @GetMapping
    fun get(@RequestParam @Min(value = 1, message = "知识库id非法") id: Long): Any
    {
        val knowledgeBase: KnowledgeBaseEntity = knowledgeBaseService.getById(id)
            ?: return ResultCode.RESOURCE_NOT_FOUND
        if (knowledgeBase.status != ArticleStatus.PUBLISHED)
        {
            if (knowledgeBase.status == ArticleStatus.ONLY_FANS)
            {
                if (userFollowService.hasFollow(StpUtil.getLoginIdAsLong(), knowledgeBase.userId!!))
                {
                    val directoryTree: List<DirectoryTreeDTO> = directoryService.getDirectoryTreeByKbId(id)
                    return Pair(knowledgeBase, directoryTree)

                }else
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
        val directoryTree: List<DirectoryTreeDTO> = directoryService.getDirectoryTreeByKbId(id)
        return Pair(knowledgeBase, directoryTree)
    }

    @SaIgnore
    @GetMapping("/basic")
    fun getBasic(@RequestParam @Min(value = 1, message = "知识库id非法") id: Long): Any
    {
        val knowledgeBase: KnowledgeBaseEntity = knowledgeBaseService.getById(id)
            ?: return ResultCode.RESOURCE_NOT_FOUND
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


    @SaIgnore
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
                    eq("status", ArticleStatus.PUBLISHED).or().eq("status",ArticleStatus.ONLY_FANS)
                }
            } else
            {
                eq("status", ArticleStatus.PUBLISHED).or().eq("status",ArticleStatus.ONLY_FANS)
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
        }
        val result = knowledgeBaseService.page(pageParam, queryWrapper)
        return PageResponse(
            records = result.records,
            total = result.total,
            pages = result.pages,
            current = result.current,
            size = result.size
        )
    }


    @SaIgnore
    @GetMapping("/index")
    fun getIndex(@RequestParam @Min(value = 1, message = "知识库id非法") id: Long): Any
    {
        val query = QueryWrapper<KnowledgeBaseEntity>().select("index", "id").eq("id", id)
        return knowledgeBaseService.getOne(query) ?: ResultCode.RESOURCE_NOT_FOUND
    }


    @SaCheckPermission("kb.post")
    @PostMapping
    fun createKnowledgeBase(@RequestBody @Valid request: CreateKnowledgeBaseRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val kbEntity = KnowledgeBaseEntity(
            userId = userId,
            name = sensitiveWord.replace(request.name),
            index = request.index,
            coverUrl = request.coverUrl
        )
        if (knowledgeBaseService.save(kbEntity))
        {
            return kbEntity
        }
        return ResultCode.DB_ERROR
    }

    @SaCheckRole("user")
    @PutMapping
    fun updateKnowledgeBase(@RequestBody @Valid request: UpdateKnowledgeBaseRequest): ResultCode
    {
        val userId = StpUtil.getLoginIdAsLong()
        val updateWrapper = UpdateWrapper<KnowledgeBaseEntity>()
        updateWrapper.eq("id", request.id)
        updateWrapper.eq("user_id", userId)
        val kbEntity = KnowledgeBaseEntity(
            status = request.status,
            name = request.name,
            index = sensitiveWord.replace(request.content),
            coverUrl = request.coverUrl
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

    @SaIgnore
    @PostMapping("/cursor")
    fun cursor(@RequestBody request: CursorKbRequest):Any
    {
        return knowledgeBaseService.cursor(request.lastCreatedAt,request.lastLike,request.lastId,request.pageSize)
    }
}
