package com.cainsgl.user.log

import com.cainsgl.common.entity.user.UserLogEntity

interface LogHandler
{
    fun supportType(): String
    fun handle(userLogEntity: UserLogEntity):Boolean
}