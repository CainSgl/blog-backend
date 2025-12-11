package com.cainsgl.user.log.handler

import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.user.log.BaseLogHandler
import org.springframework.stereotype.Component

@Component
class ViewLogHandler: BaseLogHandler("article.view") {
    override fun handle(userLogEntity: UserLogEntity): Boolean
    {
        TODO("Not yet implemented")
    }
}
@Component
class LikeLogHandler: BaseLogHandler("article.like") {
    override fun handle(userLogEntity: UserLogEntity): Boolean
    {
        TODO("Not yet implemented")
    }
}
@Component
class ReportLogHandler: BaseLogHandler("article.report") {
    override fun handle(userLogEntity: UserLogEntity): Boolean
    {
        TODO("Not yet implemented")
    }
}
@Component
class DislikeLogHandler: BaseLogHandler("article.dislike") {
    override fun handle(userLogEntity: UserLogEntity): Boolean
    {
        TODO("Not yet implemented")
    }
}
@Component
class UnlikeLogHandler: BaseLogHandler("article.unlike") {
    override fun handle(userLogEntity: UserLogEntity): Boolean
    {
        TODO("Not yet implemented")
    }
}