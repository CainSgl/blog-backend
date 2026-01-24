package com.cainsgl.article.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.cainsgl.article.service.PostOperationServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.OperateType
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.common.entity.article.PostOperationEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
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
    @GetMapping("/like")
    fun like(@RequestParam id: Long,@RequestParam add: Boolean=true,@RequestParam op: String): Any
    {
        val userId= StpUtil.getLoginIdAsLong()
        if(add)
        {
           postOperationService.save(PostOperationEntity(userId=userId,postId=id, operateType = OperateType.LIKE_TYPE.value ))
        }else
        {
            val query= UpdateWrapper<PostOperationEntity>().apply {
                eq("user_id", userId)
                eq("post_id", id)
                eq("operate_type", OperateType.LIKE_TYPE.value)
            }
            postOperationService.remove(query)

        }
        return ResultCode.SUCCESS
    }
    @GetMapping("/star")
    fun star(@RequestParam id: Long,@RequestParam add: Boolean=true): Any
    {
        val userId= StpUtil.getLoginIdAsLong()
        if(add)
        {
            postOperationService.save(PostOperationEntity(userId=userId,postId=id, operateType = OperateType.STAR.value ))
        }else
        {
            val query= UpdateWrapper<PostOperationEntity>().apply {
                eq("user_id", userId)
                eq("post_id", id)
                eq("operate_type", OperateType.STAR.value)
            }
            postOperationService.remove(query)

        }
        return ResultCode.SUCCESS
    }

}