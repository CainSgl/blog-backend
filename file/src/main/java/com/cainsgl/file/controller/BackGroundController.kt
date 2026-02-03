package com.cainsgl.file.controller

import cn.dev33.satoken.annotation.SaCheckRole
import com.cainsgl.file.FileService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.InitializingBean
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/file/welcome")
class BackGroundController : InitializingBean
{
    companion object
    {
        const val WELCOME_REDIS_PREFIX_BACKGROUND = "welcome:background"
        const val ETAG_THRESHOLD = 3
        const val URL_EXPIRES_SECONDS = 600L
    }

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Resource
    lateinit var fileService: FileService

    private var backgrounds: List<String>? = null

    override fun afterPropertiesSet()
    {
        val loadedBackgrounds = redisTemplate.opsForList().range(WELCOME_REDIS_PREFIX_BACKGROUND, 0, -1)
        if (loadedBackgrounds.isNullOrEmpty())
        {
            log.error { "无法加载背景图片列表" }
        } else
        {
            log.info { "成功加载 ${loadedBackgrounds.size} 张背景图片" }
        }
        this.backgrounds = loadedBackgrounds
    }

    /**
     * 根据索引获取背景图片URL
     */
    private fun getBackgroundUrl(index: Int): String?
    {
        val bgList = backgrounds
        if (bgList.isNullOrEmpty())
        {
            log.warn { "背景图片列表为空" }
            return null
        }
        return bgList[index % bgList.size]
    }

    /**
     * 获取欢迎页背景图片
     * 使用ETag机制优化缓存，达到阈值后重定向到预签名URL
     */
    @GetMapping("/background/{num}")
    fun getBackground(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @PathVariable num: Int
    )
    {
        val backgroundUrl = getBackgroundUrl(num)
        if (backgroundUrl.isNullOrBlank())
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "背景图片不存在")
            return
        }

        // 处理ETag缓存
        val eTag = request.getHeader("If-None-Match")
        val eTagCount = eTag?.toIntOrNull() ?: 0

        if (eTagCount in 1 until ETAG_THRESHOLD)
        {
            // 返回304继续使用缓存
            response.status = HttpServletResponse.SC_NOT_MODIFIED
            response.setHeader("ETag", (eTagCount + 1).toString())
            response.setHeader("Cache-Control", "public, max-age=3600")
            return
        }
        response.setHeader("ETag", "0")
        response.setHeader("Cache-Control", "public, max-age=3600")
        
        val downloadUrl = fileService.getDownloadUrl(
            sha256Hash = backgroundUrl,
            expiresInSeconds = URL_EXPIRES_SECONDS,
            isDownload = false,
            filename="",
            shortUrl = num % backgrounds!!.size.toLong()+1
        )
        
        response.sendRedirect(downloadUrl)
    }
    @SaCheckRole("admin")
    @GetMapping("/background/reset")
    fun reset():Any?
    {
        val loadedBackgrounds = redisTemplate.opsForList().range(WELCOME_REDIS_PREFIX_BACKGROUND, 0, -1)
        if (loadedBackgrounds.isNullOrEmpty())
        {
            log.error { "无法加载背景图片列表" }
        } else
        {
            log.info { "成功加载 ${loadedBackgrounds.size} 张背景图片" }
        }
        this.backgrounds = loadedBackgrounds
        return loadedBackgrounds
    }
}