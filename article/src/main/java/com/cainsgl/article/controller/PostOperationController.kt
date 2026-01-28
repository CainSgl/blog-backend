package com.cainsgl.article.controller

import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.api.user.UserService
import com.cainsgl.article.service.PostOperationServiceImpl
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.OperateType
import com.cainsgl.common.entity.user.UserNoticeType
import com.cainsgl.common.util.user.UserHotInfoUtils.Companion.changeLikeCount
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/post/op")
class PostOperationController
{
    @Resource
    lateinit var postOperationService: PostOperationServiceImpl

    @Resource
    lateinit var postService: PostServiceImpl
    @Resource
    lateinit var redisTemplate: RedisTemplate<Any,Any>
    //其他模块
    @Resource
    lateinit var userService: UserService
    @GetMapping("/like")
    fun like(@RequestParam id: Long, @RequestParam add: Boolean = true,@RequestParam authorId: Long): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        postOperationService.addOperate(userId = userId, type = OperateType.LIKE_TYPE, id = id, add = add)
        if (add)
        {
            redisTemplate.changeLikeCount(1,userId)
            postService.addLikeCount(id, 1)
            userService.createNotice(id, UserNoticeType.LIKE_POST.type, authorId,targetUser=userId)
        } else
        {
            redisTemplate.changeLikeCount(1,userId)
            postService.addLikeCount(id, -1)
        }
        return ResultCode.SUCCESS
    }

    @GetMapping("/star")
    fun star(@RequestParam id: Long, @RequestParam type: String, @RequestParam add: Boolean = true): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val opType = OperateType.getByOperate(type)
        postOperationService.addOperate(userId = userId, type = opType, id = id, add = add)
        if (opType == OperateType.STAR)
        {
            if (add)
            {
                postService.addStarCount(id, 1)
            } else
            {
                postService.addStarCount(id, -1)
            }
        }
        return ResultCode.SUCCESS
    }
}