package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.cainsgl.api.article.kb.KnowledgeBaseService
import com.cainsgl.api.article.post.PostService
import com.cainsgl.common.dto.response.PageResponse
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import com.cainsgl.common.entity.user.UserCollectEntity
import com.cainsgl.common.entity.user.UserGroupEntity
import com.cainsgl.user.dto.request.PageCollectRequest
import com.cainsgl.user.dto.request.PostCollectRequest
import com.cainsgl.user.dto.response.vo.CollectType
import com.cainsgl.user.service.UserCollectServiceImpl
import com.cainsgl.user.service.UserGroupServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/user/collect")
class UserCollectController {

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Resource
    lateinit var userCollectService: UserCollectServiceImpl

    @Resource
    lateinit var userGroupServiceImpl: UserGroupServiceImpl
    //其他模块的
    @Resource
    lateinit var knowledgeService: KnowledgeBaseService
    @Resource
    private lateinit var postService: PostService

    @PostMapping
    fun collect(@RequestBody request: PostCollectRequest): Any {
        val userId = StpUtil.getLoginIdAsLong()
        //获取group
        val userGroupEntity: UserGroupEntity = userGroupServiceImpl.getById(request.groupId)
            ?: return ResultCode.RESOURCE_NOT_FOUND


        if (userGroupEntity.userId != userId) {
            log.error { "用户${userId}尝试将${request.targetId}添加到分组${request.groupId}，但该分组不属于该用户" }
            return ResultCode.PERMISSION_DENIED
        }
        transactionTemplate.execute {
            val update= UpdateWrapper<UserGroupEntity>().apply {
                eq("id",userGroupEntity.id)
                setSql("count = count + 1");
            }
            userGroupServiceImpl.update(update)
            val collect = UserCollectEntity(userId = userId, targetId = request.targetId, groupId = request.groupId)
            userCollectService.save(collect)
        }
        return ResultCode.SUCCESS
    }

    @DeleteMapping
    fun delete(@RequestParam id: Long,@RequestParam type: String): ResultCode {
        val userId = StpUtil.getLoginIdAsLong()
       return transactionTemplate.execute {
           userCollectService.deleteByTargetIdAndType(userId, targetId = id, type)
          return@execute ResultCode.SUCCESS
        }?:ResultCode.DB_ERROR
    }


    @PostMapping("/page")
    fun page(@RequestBody request: PageCollectRequest): Any {
        val userGroupEntity: UserGroupEntity = userGroupServiceImpl.getById(request.id)
            ?: return ResultCode.RESOURCE_NOT_FOUND
        //为什么不取反，这里是因为publish可能是null
        val userId= StpUtil.getLoginIdAsLong()
        if (userGroupEntity.publish != true) {
            //检测是不是当前用户
            if (userGroupEntity.userId != userId) {
                return ResultCode.PERMISSION_DENIED
            }
        }
        val pageParam = Page<UserCollectEntity>(request.page, request.pageSize).apply {
            if (request.page == 1L) {
                setSearchCount(true)
            }
        }
        val queryWrapper = QueryWrapper<UserCollectEntity>()
        queryWrapper.eq("user_id", userId)
        queryWrapper.eq("group_id",userGroupEntity.id)
        val result = userCollectService.page(pageParam, queryWrapper)
        val records = result.records
        val ids = records.mapNotNull { it.targetId }
        //去把里面的targetId替换为实际的数据
        var newRecords: List<Any> = emptyList()
        if (CollectType.fromNumber(userGroupEntity.type!!) == CollectType.KB) {
            val kbs = knowledgeService.getByIds(ids)
            val idToEntityMap = kbs.associateBy { it.id }
            //将records组装下
            newRecords = records.map {
                val res = mutableMapOf<String, Any?>()
                res["target"] = idToEntityMap[it.targetId]
                res["collect"]=it
                res["type"]=CollectType.KB.str
                return@map res;
            }
        }
        if (CollectType.fromNumber(userGroupEntity.type!!) == CollectType.POST) {
            val posts = postService.getByIds(ids)
            val idToEntityMap = posts.associateBy { it.id }
            //将records组装下
            newRecords = records.map {
                val res = mutableMapOf<String, Any?>()
                res["target"] = idToEntityMap[it.targetId]
                res["collect"]=it
                res["type"]=CollectType.POST.str
                return@map res;
            }
        }
        return PageResponse(
            records = newRecords,
            total = result.total,
            pages = result.pages,
            current = result.current,
            size = result.size,
            hasMore = result.size >= request.pageSize
        )
    }


}