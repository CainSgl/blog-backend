package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.cainsgl.common.dto.response.PageResponse
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserCollectEntity
import com.cainsgl.common.entity.user.UserGroupEntity
import com.cainsgl.user.dto.request.PageCollectRequest
import com.cainsgl.user.dto.request.PostCollectRequest
import com.cainsgl.user.service.UserCollectServiceImpl
import com.cainsgl.user.service.UserGroupServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/user/collect")
class UserCollectController
{
    @Resource
    lateinit var userCollectService: UserCollectServiceImpl
    @Resource
    lateinit var userGroupServiceImpl: UserGroupServiceImpl
    @PostMapping
    fun collect(@RequestBody request: PostCollectRequest):Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        //获取group
        val userGroupEntity: UserGroupEntity = userGroupServiceImpl.getById(request.groupId)
            ?: return ResultCode.RESOURCE_NOT_FOUND
        if(userGroupEntity.userId!=userId)
        {
            log.error { "用户${userId}尝试将${request.targetId}添加到分组${request.groupId}，但该分组不属于该用户" }
            return ResultCode.PERMISSION_DENIED
        }
        val collect=UserCollectEntity(userId=userId,targetId=request.targetId,groupId=request.groupId)
        userCollectService.save(collect)
        return ResultCode.SUCCESS
    }
    @DeleteMapping
    fun delete(@RequestParam id: Long,@RequestParam groupId: Long): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val userGroupEntity: UserGroupEntity = userGroupServiceImpl.getById(groupId)
            ?: return ResultCode.RESOURCE_NOT_FOUND
        if(userGroupEntity.userId!=userId)
        {
            log.error { "用户${userId}尝试删除分组${groupId}中的收藏${id}，但该分组不属于该用户" }
            return ResultCode.PERMISSION_DENIED
        }
        val queryWrapper = QueryWrapper<UserCollectEntity>().apply {
            eq("group_id", groupId)
            eq("id", id)
        }
        if(userCollectService.removeById(queryWrapper))
        {
            return ResultCode.SUCCESS
        }
        return ResultCode.DB_ERROR
    }



    @PostMapping("/page")
    fun page(@RequestBody request: PageCollectRequest): Any
    {
        val userGroupEntity: UserGroupEntity = userGroupServiceImpl.getById(request.id)
            ?: return ResultCode.RESOURCE_NOT_FOUND
        //别问为什么不取反，这里是因为publish可能是null
        if(userGroupEntity.publish != true)
        {
            //检测是不是当前用户
            if(userGroupEntity.userId != StpUtil.getLoginIdAsLong())
            {
                return ResultCode.PERMISSION_DENIED
            }
        }
        val pageParam = Page<UserCollectEntity>(request.page, request.pageSize).apply {
            if (request.page == 1L)
            {
                setSearchCount(false)
            }
        }
        val queryWrapper = QueryWrapper<UserCollectEntity>()
        queryWrapper.eq("user_id", userGroupEntity.id)
        val result = userCollectService.page(pageParam,queryWrapper)
        return PageResponse(
            records = result.records,
            total = result.total,
            pages = result.pages,
            current = result.current,
            size = result.size,
            hasMore=result.size>=request.pageSize
        )
    }






}