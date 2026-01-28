package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.common.dto.request.CursorList
import com.cainsgl.common.dto.request.OnlyId
import com.cainsgl.user.dto.response.FollowUserResponse
import com.cainsgl.user.service.UserExtraInfoServiceImpl
import com.cainsgl.user.service.UserFollowServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/follow")
class FollowController
{
    @Resource
    private lateinit var userFollowService: UserFollowServiceImpl

    @Resource
    lateinit var userHotInfoService: UserExtraInfoServiceImpl

    @Resource
    lateinit var transactionTemplate: TransactionTemplate


    @GetMapping
    fun hasFollow(@RequestParam id: Long): Any
    {
        if (!StpUtil.isLogin())
        {
            return false
        }
        val userId = StpUtil.getLoginIdAsLong()
        if(userId ==id)
        {
            return true
        }
        return userFollowService.checkFollowing(userId, id)
    }


    @PostMapping("/er/list")
    fun getFollower(@RequestBody request: CursorList): List<FollowUserResponse>
    {
        //每次默认返回20条
        return userFollowService.getFollowerUsers(request.id, request.lastId)
    }
    @PostMapping("/ee/list")
    fun getFollowee(@RequestBody request: CursorList): List<FollowUserResponse>
    {
        return userFollowService.getFolloweeUsers(request.id, request.lastId)
    }



    @DeleteMapping
    fun unFollow(@RequestParam id: Long): Boolean
    {
        val userId = StpUtil.getLoginIdAsLong()
        if(userId ==id)
        {
            return false
        }
        transactionTemplate.execute {
            if (userFollowService.unfollow(userId, id))
            {
                userHotInfoService.decrFlowCount(userId, id)
            }
        }
        return true
    }


    @PostMapping
    fun addFollow(@RequestBody request: OnlyId):Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        if(userId ==request.id)
        {
            return false
        }
        transactionTemplate.execute {
            if (userFollowService.follow(userId, request.id))
            {
                userHotInfoService.incrFlowCount(userId, request.id)
            }

        }
        return true
    }
}