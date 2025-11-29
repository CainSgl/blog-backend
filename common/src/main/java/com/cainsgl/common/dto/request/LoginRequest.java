package com.cainsgl.common.dto.request;

import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Data
public class LoginRequest {
    
    /**
     * 用户名或邮箱
     */
    private String account;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 校验参数是否完整
     * @return true-参数完整，false-参数不全
     */
    public boolean validate() {
        return StringUtils.hasText(account)&&StringUtils.hasText(password);
    }
    
    /**
     * 根据当前请求的 User-Agent 判断设备类型
     * @return pc, mobile, tablet 三种类型之一
     */
    public static String getDeviceType() {
        String userAgent = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            userAgent = attributes.getRequest().getHeader("User-Agent");
        }
        
        if (userAgent == null || userAgent.isEmpty()) {
            return "pc"; // 默认为 PC
        }
        
        String ua = userAgent.toLowerCase();
        // 判断是否为平板
        if (ua.contains("tablet") || ua.contains("ipad")) {
            return "tablet";
        }
        // 判断是否为移动设备
        if (ua.contains("mobile") || 
            ua.contains("android") || 
            ua.contains("iphone") || 
            ua.contains("ipod") || 
            ua.contains("blackberry") || 
            ua.contains("windows phone")) {
            return "mobile";
        }
        // 默认为 PC
        return "pc";
    }
}
