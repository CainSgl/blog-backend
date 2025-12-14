package com.cainsgl.consumer.user.log.handler

import com.cainsgl.consumer.user.log.BaseLogHandler
import com.cainsgl.consumer.user.log.context.LogProcessContext
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
