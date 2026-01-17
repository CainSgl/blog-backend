package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserGroupEntity
import com.cainsgl.user.dto.request.PostGroupRequest
import com.cainsgl.user.dto.request.PutGroupRequest
import com.cainsgl.user.service.UserGroupServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*
private val log = KotlinLogging.logger {}
@RestController
@RequestMapping("/user/group")
class UserGroupController
{
    @Resource
    lateinit var userGroupServiceImpl: UserGroupServiceImpl
    @GetMapping
    fun get(@RequestParam type: String?): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val groupMap = userGroupServiceImpl.getByUserIdAndType(userId, type)
        return groupMap
    }
    @GetMapping("/other")
    fun get(@RequestParam type: String?,@RequestParam userId:Long): Any
    {
        val groupMap = userGroupServiceImpl.getByUserIdAndType(userId, type,true)
        return groupMap
    }
    @PostMapping
    fun post(@RequestBody request: PostGroupRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val group = userGroupServiceImpl.addGroup(userId, request.type, request.name)
        return group
    }
    @DeleteMapping
    fun delete(@RequestParam id: Long): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val query= QueryWrapper<UserGroupEntity>().apply {
            eq("user_id",userId)
            eq("id",id)
        }
        userGroupServiceImpl.remove(query)
        return ResultCode.SUCCESS
    }
    @PutMapping
    fun put(@RequestBody request: PutGroupRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val query= UpdateWrapper<UserGroupEntity>().apply {
            eq("id",request.id)
            eq("user_id",userId)
            set("name",request.name)
        }
        userGroupServiceImpl.update(query)
        return ResultCode.SUCCESS
    }
}