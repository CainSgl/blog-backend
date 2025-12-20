package com.cainsgl.consumer.user.log.handler

import com.cainsgl.api.article.post.PostService
import com.cainsgl.api.user.extra.UserExtraInfoService
import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.common.exception.BSystemException
import com.cainsgl.common.util.VectorUtils
import com.cainsgl.consumer.user.log.BaseLogHandler
import com.cainsgl.consumer.user.log.context.LogPostProcessor
import com.cainsgl.consumer.user.log.context.LogProcessContext
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

const val LIKE = 0.2f
const val VIEW = 0.05f
const val DISLIKE = -0.2f
const val UNLIKE = -0.2f
const val REPORT = -0.4f


private val logger = KotlinLogging.logger {}
@Component
class MergeVectorManger
{
    @Resource
    lateinit var postService: PostService

    fun mergeWithArticleVector(interestVector: FloatArray, value: Float, log: UserLogEntity)
    {
        if (log.info == null)
        {
            throw BSystemException("无法处理的用户日志,$log,因为info为null")
        }
        val postId = log.info!!["postId"] as? Number
            ?: throw BSystemException("无法处理的用户日志,$log,因为无法获取到info.postId")
        val postVector = postService.getVectorById(postId.toLong())
        if (postVector == null)
        {
            //可能是还没发布的文章，不管
            logger.warn { "未发布的文章产生了日志$postId" }
            return
        }
        for (index in interestVector.indices)
        {
            interestVector[index] += postVector[index] * value
        }
    }
}

private val log = KotlinLogging.logger {}
open class BaseArticleLogHandler(supportType: String, private val value: Float) : BaseLogHandler(supportType)
{
    @Resource
    lateinit var mergeVector: MergeVectorManger

    @Resource
    lateinit var userExtraInfo: UserExtraInfoService

    override fun handle(context: LogProcessContext)
    {
        val user: UserLogEntity = context.current()
        val userId = user.userId ?: run {
            log.warn { "错误的用户日志，里面缺少数据，userId" }
            return
        }
        val articleKey = "article.$userId"
        var interestVectorObj = context.getAttribute(articleKey)
        if (interestVectorObj == null) {
            fillUserInterestVector(context, articleKey, userId)
            // 重新获取填充后的向量（兜底：防止fill逻辑异常导致null）
            interestVectorObj = context.getAttribute(articleKey)
        }
        // 最终校验：向量仍不存在则返回
        val interestVector= interestVectorObj as? FloatArray ?: return
        mergeVector.mergeWithArticleVector(interestVector, value, user)
        context.addPostProcessor(LogPostProcessor("article", ::postProcess))
    }
    private fun fillUserInterestVector(context: LogProcessContext, articleKey: String, userId: Long) {
        val interestVector:FloatArray? = userExtraInfo.getInterestVector(userId)
        if (interestVector == null||interestVector.isEmpty()) {
            log.warn { "获取到的用户[$userId]热信息兴趣偏好向量为null" }
            return
        }
        context.setAttribute(articleKey, interestVector)
    }


    private fun postProcess(context: LogProcessContext)
    {
        for (attribute in context.getAttributes())
        {
            val key = attribute.key
            if(!key.startsWith("article."))
            {
                continue
            }
            val userIdStr = key.substringAfter("article.")
            val userId = try {
                userIdStr.toLong()
            } catch (e: NumberFormatException) {
                log.error(e) { "解析用户ID失败，非法Key格式：$key" }
                continue // 解析失败，跳过当前Key
            }
            val interestVector = attribute.value as? FloatArray
            if (interestVector == null) {
                log.warn { "用户[$userId]兴趣向量为空（Key：$key），跳过保存" }
                continue
            }
            val saveSuccess = userExtraInfo.setInterestVector(userId, VectorUtils.l2Normalize(interestVector))
            if (saveSuccess) {
                log.info { "用户[$userId]兴趣向量已更新成功（Key：$key）" }
            } else {
                log.warn { "用户[$userId]兴趣向量更新失败（Key：$key）" }
            }
        }
    }
}

@Component
class ViewLogHandler : BaseArticleLogHandler("article.view", VIEW)
{
    @Resource
    lateinit var postService: PostService


    override fun handle(context: LogProcessContext)
    {
        super.handle(context)
        val id= context.current().id
        //这里还需要增加文章的阅读量
        val v = context.getAttribute("view.$id") as? Int
        if(v == null)
        {
            context.setAttribute("view.$id", 1)
            return
        }
        context.setAttribute("view.$id", v+1)
        context.addPostProcessor(LogPostProcessor("view", ::viewPostProcess))
    }
    fun viewPostProcess(context: LogProcessContext)
    {
        for (attribute in context.getAttributes())
        {
            val key = attribute.key
            if(!key.startsWith("view."))
            {
                continue
            }
            val articleIdStr = key.substringAfter("view.")
            val articleId = try {
                articleIdStr.toLong()
            } catch (e: NumberFormatException) {
                log.error(e) { "解析用户ID失败，非法Key格式：$key" }
                continue // 解析失败，跳过当前Key
            }
            val  count =  attribute.value as? Int
            if(count==null||count<1)
            {
                return
            }
            //去增加他的count
            postService.addViewCount(articleId, count=count)
        }
    }

}

@Component
class LikeLogHandler : BaseArticleLogHandler("article.like", LIKE)

@Component
class ReportLogHandler : BaseArticleLogHandler("article.report", REPORT)

@Component
class DislikeLogHandler : BaseArticleLogHandler("article.dislike", DISLIKE)

@Component
class UnlikeLogHandler : BaseArticleLogHandler("article.unlike", UNLIKE)
