package com.cainsgl.user.log

abstract class BaseLogHandler : LogHandler
{
    private val supportType: String
    constructor(supportType: String)
    {
        this.supportType = supportType
    }
    override fun supportType(): String
    {
        return supportType
    }
}
