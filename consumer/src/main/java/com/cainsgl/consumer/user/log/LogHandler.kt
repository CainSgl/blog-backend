package com.cainsgl.user.log

import com.cainsgl.user.log.context.ProcessContext

interface LogHandler
{
    fun supportType(): String
    fun process(context: ProcessContext)
}