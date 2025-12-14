package com.cainsgl.consumer.user.log

import com.cainsgl.consumer.user.log.context.ProcessContext

interface LogHandler
{
    fun supportType(): String
    fun process(context: ProcessContext)
}