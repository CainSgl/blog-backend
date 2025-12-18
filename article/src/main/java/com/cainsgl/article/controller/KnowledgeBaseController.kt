package com.cainsgl.article.controller

import cn.dev33.satoken.annotation.SaCheckPermission
import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.cainsgl.article.dto.DirectoryTreeDTO
import com.cainsgl.article.dto.request.CreateKnowledgeBaseRequest
import com.cainsgl.article.dto.request.UpdateKnowledgeBaseRequest
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.article.service.KnowledgeBaseServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import jakarta.annotation.Resource
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
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
    fun get(@RequestParam @Min(value = 0, message = "知识库id不能小于0") id: Long): Any
    {
        val knowledgeBase: KnowledgeBaseEntity = knowledgeBaseService.getById(id)
            ?: return ResultCode.RESOURCE_NOT_FOUND
        val directoryTree: List<DirectoryTreeDTO> = directoryService.getDirectoryTreeByKbId(id)
        return Pair(knowledgeBase, directoryTree)
    }

    @SaCheckPermission("kb.post")
    @PostMapping
    fun createKnowledgeBase(@RequestBody @Valid request: CreateKnowledgeBaseRequest): ResultCode
    {
        val userId = StpUtil.getLoginIdAsLong()
        val kbEntity = KnowledgeBaseEntity(userId=userId, name = request.name)
        if (knowledgeBaseService.save(kbEntity))
        {
            return ResultCode.SUCCESS
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
        val kbEntity = KnowledgeBaseEntity(status = request.status, name = request.name)
        if (knowledgeBaseService.update(kbEntity, updateWrapper))
        {
            return ResultCode.SUCCESS
        }
        return ResultCode.DB_ERROR
    }
}
