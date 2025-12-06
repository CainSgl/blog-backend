package com.cainsgl.article.controller

import cn.dev33.satoken.annotation.SaCheckPermission
import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper
import com.cainsgl.article.dto.DirectoryTreeDTO
import com.cainsgl.article.dto.request.kb.CreateKnowledgeBaseRequest
import com.cainsgl.article.dto.request.kb.UpdateKnowledgeBaseRequest
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.article.service.KnowledgeBaseServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/kb")
class KnowledgeBaseController
{

    @Resource
    private lateinit var redisTemplate: RedisTemplate<Any, Any>

    @Resource
    lateinit var knowledgeBaseService: KnowledgeBaseServiceImpl
    @Resource
    lateinit var directoryService: DirectoryServiceImpl
    @SaCheckRole("user")
    @GetMapping
    fun get(@RequestParam(required = false) id: Long?): Any
    {
        requireNotNull(id) { return ResultCode.MISSING_PARAM }
        val knowledgeBase: KnowledgeBaseEntity = knowledgeBaseService.getById(id)
            ?: return ResultCode.RESOURCE_NOT_FOUND
        val directoryTree: List<DirectoryTreeDTO> = directoryService.getDirectoryTreeByKbId(id)
        return Pair(knowledgeBase, directoryTree)
    }

    @SaCheckPermission("kb.post")
    @PostMapping
    fun createKnowledgeBase(@RequestBody request: CreateKnowledgeBaseRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val kbEntity = KnowledgeBaseEntity(userId, name = request.name)
        if (knowledgeBaseService.save(kbEntity))
        {
            return ResultCode.SUCCESS
        }
        return ResultCode.DB_ERROR
    }

    @SaCheckRole("user")
    @PutMapping
    fun updateKnowledgeBase(@RequestBody request: UpdateKnowledgeBaseRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val updateWrapper = LambdaUpdateWrapper<KnowledgeBaseEntity>()
        updateWrapper.eq(KnowledgeBaseEntity::id, request.id)
        updateWrapper.eq(KnowledgeBaseEntity::userId, userId)
        val kbEntity = KnowledgeBaseEntity(status = request.status, name = request.name)
        if (knowledgeBaseService.update(kbEntity, updateWrapper))
        {
            return ResultCode.SUCCESS
        }
        return ResultCode.DB_ERROR
    }
}
