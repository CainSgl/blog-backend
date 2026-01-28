package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserCollectEntity
import com.cainsgl.common.entity.user.UserGroupEntity
import com.cainsgl.common.exception.BSystemException
import com.cainsgl.user.dto.request.PostGroupRequest
import com.cainsgl.user.dto.request.PutGroupRequest
import com.cainsgl.user.service.UserCollectServiceImpl
import com.cainsgl.user.service.UserGroupServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/user/group")
class UserGroupController {
    @Resource
    lateinit var userGroupServiceImpl: UserGroupServiceImpl
    @Resource
    lateinit var userCollectService: UserCollectServiceImpl
    @Resource
    lateinit var transactionTemplate: TransactionTemplate
    @GetMapping
    fun get(@RequestParam type: String?,@RequestParam userId: Long?): Any {
        val requestUserId = StpUtil.getLoginIdAsLong()
        return if(userId==null) {
            userGroupServiceImpl.getByUserIdAndType(requestUserId, type,needPublish= false)
        }else {
            userGroupServiceImpl.getByUserIdAndType(userId, type,needPublish=userId!=requestUserId)
        }
    }



    @PostMapping
    fun post(@RequestBody request: PostGroupRequest): Any {
        val userId = StpUtil.getLoginIdAsLong()
        val group = userGroupServiceImpl.addGroup(userId, request.type, request.name, request.description, publish = request.publish)
        return group
    }

    @DeleteMapping
    fun delete(@RequestParam id: Long): Any {
        val userId = StpUtil.getLoginIdAsLong()
        val query = KtQueryWrapper(UserGroupEntity::class.java).apply {
            eq(UserGroupEntity::userId, userId)
            eq(UserGroupEntity::id, id)
        }
        transactionTemplate.execute {
            val userGroup= userGroupServiceImpl.getOne(query)
            if(userGroup != null) {
                if (userGroupServiceImpl.remove(query)) {
                    if(userGroup.id==null)
                    {
                        throw BSystemException("获取到的userGroup为null，出乎意料的bug")
                    }
                    val query = KtQueryWrapper(UserCollectEntity::class.java).apply {
                        eq(UserCollectEntity::userId, userId)
                        eq(UserCollectEntity::groupId, userGroup.id)
                    }
                    userCollectService.remove(query)
                }
            }
        }
        return ResultCode.SUCCESS
    }

    @PutMapping
    fun put(@RequestBody request: PutGroupRequest): Any {
        val userId = StpUtil.getLoginIdAsLong()
        val query = KtUpdateWrapper(UserGroupEntity::class.java).apply {
            eq(UserGroupEntity::id, request.id)
            eq(UserGroupEntity::userId, userId)
            if (request.name != null)
                set(UserGroupEntity::name, request.name)
            if (request.description != null)
                set(UserGroupEntity::description, request.description)
            if (request.publish != null)
                set(UserGroupEntity::publish, request.publish)
        }
        userGroupServiceImpl.update(query)
        return ResultCode.SUCCESS
    }
}