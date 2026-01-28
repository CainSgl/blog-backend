package com.cainsgl.article.controller

import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.cainsgl.article.service.PostHistoryServiceImpl
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.dto.request.OnlyId
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.PostHistoryEntity
import jakarta.annotation.Resource
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/post")
class PostHistoryController
{
    @Resource
    lateinit var postService: PostServiceImpl

    @Resource
    lateinit var postHistoryService: PostHistoryServiceImpl

    @SaCheckRole("user")
    @GetMapping("/last")
    fun getByLast(@RequestParam id: Long): Any
    {
        val post = postService.getPostBaseInfo(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        val userId = StpUtil.getLoginIdAsLong()
        if (post.userId != userId)
        {
            //不是他的，这个是历史版本的缓存版本
            return ResultCode.PERMISSION_DENIED
        }
        val historyQuery = KtQueryWrapper(PostHistoryEntity::class.java)
            .select(PostHistoryEntity::content)
            .eq(PostHistoryEntity::postId, post.id).eq(PostHistoryEntity::userId, userId).orderByDesc(PostHistoryEntity::version).last("LIMIT 1")
        val one = postHistoryService.getOne(historyQuery)
        //检查用户是否有权访问
        post.content = one?.content
        return post
    }

    //这个是别人看的，直接走缓存
    @PostMapping("/history")
    fun history(@RequestBody @Valid request: OnlyId): List<PostHistoryEntity>
    {
        return postHistoryService.getByCache(request.id) ?: emptyList()
    }
    @SaCheckRole
    @GetMapping("/history")
    fun getHistory(@RequestParam id: Long,@RequestParam postId:Long): Any
    {
       return postHistoryService.getContentByIdAndPostIdWithNonMaxVersion(id,postId)?:return ResultCode.PERMISSION_DENIED
    }

    @SaCheckRole
    @PostMapping("/history/self")
    fun historySelf(@RequestBody @Valid request: OnlyId): List<PostHistoryEntity>
    {
        val historyQuery = KtQueryWrapper(PostHistoryEntity::class.java)
            .eq(PostHistoryEntity::postId, request.id).eq(PostHistoryEntity::userId, StpUtil.getLoginIdAsLong()).orderByDesc(PostHistoryEntity::version)
        return postHistoryService.list(historyQuery)
    }
}