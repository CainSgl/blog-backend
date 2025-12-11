package com.cainsgl.user.log.handler

import com.cainsgl.common.entity.user.UserLogEntity
import com.cainsgl.user.log.BaseLogHandler
import org.springframework.stereotype.Component

@Component
class FollowLogHandler: BaseLogHandler("user.follow") {
    override fun handle(userLogEntity: UserLogEntity): Boolean
    {
        TODO("Not yet implemented")
    }
}
@Component
class UnfollowLogHandler: BaseLogHandler("user.unfollow") {
    override fun handle(userLogEntity: UserLogEntity): Boolean
    {
        TODO("Not yet implemented")
    }
}
