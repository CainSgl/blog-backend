package com.cainsgl.user.log.handler

import com.cainsgl.user.log.BaseLogHandler
import com.cainsgl.user.log.context.LogProcessContext
import org.springframework.stereotype.Component

@Component
class FollowLogHandler: BaseLogHandler("user.follow") {
    override fun handle(context: LogProcessContext)
    {
        TODO("Not yet implemented")
    }
}
@Component
class UnfollowLogHandler: BaseLogHandler("user.unfollow") {
    override fun handle(context: LogProcessContext)
    {
        TODO("Not yet implemented")
    }
}
