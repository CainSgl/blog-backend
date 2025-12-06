package com.cainsgl.article.controller






import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.ArticleStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.apache.rocketmq.client.core.RocketMQClientTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/post")
class PostController
{

    @Resource
    lateinit var postService: PostServiceImpl

    @Resource
    private lateinit var rocketMQClientTemplate: RocketMQClientTemplate


    /**
     * 根据ID获取文章
     */
    @GetMapping
    fun get(@RequestParam(required = false) id: Long?): Any
    {
        if (id == null)
        {
            return ResultCode.MISSING_PARAM
        }
        val post = postService.getPost(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        //检查用户是否有权访问
        if (post.status == ArticleStatus.PUBLISHED)
        {
            return post
        }
        //限制为只允许该用户访问
        val userId = StpUtil.getLoginIdAsLong()
        if (userId == post.userId)
        {
            return post
        }
        return ResultCode.PERMISSION_DENIED
    }









}
