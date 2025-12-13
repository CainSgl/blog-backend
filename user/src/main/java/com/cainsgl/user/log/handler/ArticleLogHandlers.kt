package com.cainsgl.user.log.handler

import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.common.exception.BSystemException
import com.cainsgl.user.log.BaseLogHandler
import com.cainsgl.user.log.context.LogProcessContext
import com.cainsgl.user.service.UserExtraInfoServiceImpl
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

const val LIKE = 0.2f
const val VIEW = 0.05f
const val DISLIKE = -0.2f
const val UNLIKE = -0.2f
const val REPORT = -0.4f

@Component
class ViewLogHandler : BaseLogHandler("article.view")
{
    @Resource
    lateinit var userExtraInfoServiceImpl: UserExtraInfoServiceImpl

    override fun handle(context: LogProcessContext)
    {
        val user: UserLogEntity = context.current()
        val attribute = context.getAttribute("article") as FloatArray
        if (attribute != null)
        {
            //增加兴趣向量
            addXinQvVector(attribute,VIEW)
        } else
        {
            //说明没有，必须添加进去
            if (user.id == null)
            {
                throw BSystemException("无法处理的用户article.view日志，该日志存储的userId是null")
            }
            val interestVector = userExtraInfoServiceImpl.getInterestVector(user.id!!)
                ?: throw BSystemException("无法处理的用户article.view日志，无法从数据库找到他的兴趣向量")
            context.setAttribute("article", interestVector)
        }
    }
    fun addXinQvVector(floatArray: FloatArray,value: Float)
    {

    }
}

@Component
class LikeLogHandler : BaseLogHandler("article.like")
{
    override fun handle(context: LogProcessContext)
    {
        TODO("Not yet implemented")
    }
}

@Component
class ReportLogHandler : BaseLogHandler("article.report")
{
    override fun handle(context: LogProcessContext)
    {
        TODO("Not yet implemented")
    }
}

@Component
class DislikeLogHandler : BaseLogHandler("article.dislike")
{
    override fun handle(context: LogProcessContext)
    {
        TODO("Not yet implemented")
    }
}

@Component
class UnlikeLogHandler : BaseLogHandler("article.unlike")
{
    override fun handle(context: LogProcessContext)
    {
        TODO("Not yet implemented")
    }
}