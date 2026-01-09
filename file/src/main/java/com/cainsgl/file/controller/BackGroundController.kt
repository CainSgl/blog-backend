package com.cainsgl.file.controller

import com.cainsgl.file.FileService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.InitializingBean
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/welcome")
class BackGroundController : InitializingBean
{
    companion object
    {
        const val WELCOME_REDIS_PREFIX_BACKGROUND = "welcome:background"
    }

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, String>
    override fun afterPropertiesSet()
    {
        val backGrounds: MutableList<String>? = redisTemplate.opsForList().range(WELCOME_REDIS_PREFIX_BACKGROUND, 0, -1)
        if (backGrounds == null)
        {
            //为null，
            log.error { "could not load back grounds" }
        }
        this.backGrounds = backGrounds
    }

    @Resource
    lateinit var fileService: FileService


    private var backGrounds: List<String>? = null
    fun getRandom(): String
    {
        if (backGrounds == null)
        {
            return ""
        }
        return backGrounds!!.random()
    }

    @GetMapping("/background")
    fun getBackground(
        request: HttpServletRequest,
        response: HttpServletResponse
    )
    {
        //获取请求头里的etag
        val eTag = request.getHeader("If-None-Match")
        if (eTag != null && eTag.isNotEmpty())
        {
            val count= eTag.toInt()
          if(count>3)
          {
              val url = getRandom()
              response.setHeader("ETag", "0");
              fileService.getFile(url, false, response, "welcome.png")
          }else
          {
              response.setHeader("ETag", (count+1).toString());
              response.status = HttpServletResponse.SC_NOT_MODIFIED
              return;
          }
        }
        response.setHeader("ETag", "0");
        fileService.getFile(getRandom(), false, response, "welcome.png")
    }
}