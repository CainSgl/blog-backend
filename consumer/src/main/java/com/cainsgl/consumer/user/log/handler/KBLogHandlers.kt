package com.cainsgl.consumer.user.log.handler

import com.cainsgl.api.article.kb.KnowledgeBaseService
import com.cainsgl.common.exception.BSystemException
import com.cainsgl.consumer.user.log.BaseLogHandler
import com.cainsgl.consumer.user.log.context.LogPostProcessor
import com.cainsgl.consumer.user.log.context.LogProcessContext
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}
open class BaseKBLogHandler(supportType: String, private val value: Int) : BaseLogHandler(supportType)
{
    @Resource
    private lateinit var knowledgeBaseService: KnowledgeBaseService

    override fun handle(context: LogProcessContext)
    {
        val current = context.current()
        //获取kbId
        val kbId= current.info!!["id"] as? Long ?: throw BSystemException("无法处理的用户日志,$log,因为无法获取到info.postId")
        val kbKey="kb.$kbId"
        val likeValue = context.getAttribute(kbKey)
        if(likeValue==null)
        {
            context.setAttribute(kbKey,value)
            return
        }
        var likeCount=likeValue as Int
        likeCount+=value
        context.setAttribute(kbKey,likeCount)
        context.addPostProcessor(LogPostProcessor("kb",::postProcess))
    }
    private fun postProcess(context: LogProcessContext)
    {
        //保存数据到数据库
        for (attribute in context.getAttributes())
        {
            val key = attribute.key
            if(!key.startsWith("kb."))
            {
                continue
            }
            val kbId = key.substringAfter("kb.")
            knowledgeBaseService.addKbLikeCount(kbId.toLong(), addCount =attribute.value as Int)
        }
    }
}
@Component
class KBLikeLogHandler : BaseKBLogHandler("kb.like", 1)
@Component
class KBUnLikeLogHandler : BaseKBLogHandler("kb.unlike", -1)