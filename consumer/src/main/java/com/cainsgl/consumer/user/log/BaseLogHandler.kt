package com.cainsgl.consumer.user.log

import com.cainsgl.consumer.user.log.context.LogProcessContext
import com.cainsgl.consumer.user.log.context.ProcessContext

abstract class BaseLogHandler(private val supportType: String) : LogHandler
{
    override fun supportType(): String
    {
        return supportType
    }
    override fun process(context: ProcessContext)
    {
        handle(context as LogProcessContext)
    }
    abstract fun handle(context: LogProcessContext)
}
