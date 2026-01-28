package com.cainsgl.article.system

import cn.dev33.satoken.annotation.SaCheckRole
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.article.service.KnowledgeBaseServiceImpl
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.article.system.entity.CarouselEntity
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.common.util.FineLockCacheUtils.getWithFineLock
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.*
import java.time.Duration

@RestController
@RequestMapping("/system")
class SystemInfoController
{
    @Resource
    lateinit var carouselServiceImpl: CarouselServiceImpl

    @Resource
    lateinit var postService: PostServiceImpl

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @Resource
    lateinit var directoryService: DirectoryServiceImpl

    @Resource
    lateinit var knowledgeBaseService: KnowledgeBaseServiceImpl

    @GetMapping("/carousel")
    fun getCarousel(): List<CarouselEntity>
    {
        //去读redis的缓存，并且缓存时间必定是一天
        return carouselServiceImpl.getCarousels()
    }

    @SaCheckRole("admin")
    @DeleteMapping("/carousel")
    fun reset(): Any
    {
        carouselServiceImpl.resetCarousels()
        //去读redis的缓存，并且缓存时间必定是一天
        return ResultCode.SUCCESS
    }

    @GetMapping("/announcement")
    fun getAnnouncement(): Any
    {
        return redisTemplate.getWithFineLock("announcement", { Duration.ofHours(24) }) {
            val query = QueryWrapper<PostEntity>().select(PostEntity.BASIC_COL).eq("kb_id", 1)
            return@getWithFineLock postService.list(query)
        } ?: ResultCode.UNKNOWN_ERROR
    }

    @SaCheckRole("admin")
    @DeleteMapping("/announcement")
    fun resetAnnouncement(): Any
    {
        redisTemplate.delete("announcement")
        Thread.ofVirtual().start { Thread.sleep(3000); redisTemplate.delete("announcement") }
        return ResultCode.SUCCESS
    }

    @GetMapping("/about")
    fun getAbout(): Any
    {
        return directoryService.getDirectoryTreeByKbId(2L)
    }
    @SaCheckRole("admin")
    @DeleteMapping("/about")
    fun resetAbout(): Any
    {
        directoryService.removeCache(2L)
        return ResultCode.SUCCESS
    }
    @GetMapping("/about/content")
    fun getAboutContent(@RequestParam(required = false) id: Long?, request: HttpServletRequest, response: HttpServletResponse): Any
    {
        if (id == null)
        {
            return redisTemplate.getWithFineLock("about:index", { Duration.ofHours(24) }) {
                val query = KtQueryWrapper(KnowledgeBaseEntity::class.java).select(KnowledgeBaseEntity::index, KnowledgeBaseEntity::id).eq(KnowledgeBaseEntity::id, 2)
                return@getWithFineLock knowledgeBaseService.getOne(query).index
            } ?: ResultCode.UNKNOWN_ERROR
        }
        val post = postService.getPostBaseInfo(id)
        if (post?.kbId != 2L)
        {
            return ResultCode.RESOURCE_NOT_FOUND
        }
        val eTag = request.getHeader("If-None-Match")
        if (!eTag.isNullOrEmpty() && eTag == post.version.toString())
        {
            response.status = HttpServletResponse.SC_NOT_MODIFIED
            return ResultCode.SUCCESS
        }
        response.setHeader("ETag", post.version.toString())
        return post
    }

    @GetMapping("/announcement/content")
    fun getAnnouncementContent(
        @RequestParam id: Long, request: HttpServletRequest, response: HttpServletResponse
    ): Any
    {
        val post = postService.getPostBaseInfo(id)
        if (post?.kbId != 1L)
        {
            return ResultCode.RESOURCE_NOT_FOUND
        }
        val eTag = request.getHeader("If-None-Match")
        if (!eTag.isNullOrEmpty() && eTag == post.version.toString())
        {
            response.status = HttpServletResponse.SC_NOT_MODIFIED
            return ResultCode.SUCCESS
        }
        response.setHeader("ETag", post.version.toString())
        return post
    }

}