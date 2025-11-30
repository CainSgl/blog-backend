package com.cainsgl.user.dto.request

import org.springframework.util.StringUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

data class LoginRequest(
    /**
     * 用户名或邮箱
     */
    var account: String? = null,

    /**
     * 密码
     */
    var password: String? = null
)
{
    /**
     * 校验参数是否完整
     * @return true-参数完整，false-参数不全
     */
    fun validate(): Boolean
    {
        return StringUtils.hasText(account) && StringUtils.hasText(password)
    }

    companion object
    {
        /**
         * 根据当前请求的 User-Agent 判断设备类型
         * @return pc, mobile, tablet 三种类型之一
         */
        @JvmStatic
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
}
