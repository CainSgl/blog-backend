package com.cainsgl.article.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.cainsgl.article.dto.response.CursorResult
import com.cainsgl.article.dto.response.PostViewHistoryDTO
import com.cainsgl.article.entity.PostViewHistoryEntity
import com.cainsgl.article.service.PostViewHistoryServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/post/view/history")
class PostViewHistoryController
{
    @Resource
    lateinit var postViewHistoryService: PostViewHistoryServiceImpl

    @GetMapping
    fun cursor(
        @RequestParam(required = false) after: LocalDateTime?, @RequestParam(defaultValue = "10") limit: Int
    ): CursorResult<PostViewHistoryDTO>
    {
        var pageLimit = limit
        if (pageLimit > 50)
        {
            //目前限制成50吧
            pageLimit = 50
        }
        val userId = StpUtil.getLoginIdAsLong()
        return postViewHistoryService.getHistory(userId, after, pageLimit)
    }

    @DeleteMapping
    fun delete(@RequestParam ids:List<Long>): ResultCode
    {
        val update= UpdateWrapper<PostViewHistoryEntity>()
        val userId = StpUtil.getLoginIdAsLong()
        update.`in`("id",ids).eq("user_id", userId)
        postViewHistoryService.remove(update)
        return ResultCode.SUCCESS
    }
    @DeleteMapping("/all")
    fun deleteAll( ): ResultCode
    {
        val update= UpdateWrapper<PostViewHistoryEntity>()
        val userId = StpUtil.getLoginIdAsLong()
        update.eq("user_id", userId)
        postViewHistoryService.remove(update)
        return ResultCode.SUCCESS
    }
}
