package com.cainsgl.common.util.user

import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object DeviceUtils
{
    fun getDeviceType(): String
    {
        var userAgent: String? = null
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        if (attributes != null)
        {
            userAgent = attributes.request.getHeader("User-Agent")
        }

        if (userAgent.isNullOrEmpty())
        {
            return "pc" // 默认为 PC
        }

        val ua = userAgent.lowercase()
        // 判断是否为平板
        if (ua.contains("tablet") || ua.contains("ipad"))
        {
            return "tablet"
        }
        // 判断是否为移动设备
        if (ua.contains("mobile") ||
            ua.contains("android") ||
            ua.contains("iphone") ||
            ua.contains("ipod") ||
            ua.contains("blackberry") ||
            ua.contains("windows phone")
        )
        {
            return "mobile"
        }
        // 默认为 PC
        return "pc"
    }
}