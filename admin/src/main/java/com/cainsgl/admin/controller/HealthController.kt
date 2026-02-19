package com.cainsgl.admin.controller

import cn.dev33.satoken.annotation.SaCheckRole
import com.cainsgl.admin.service.HealthCheckService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class HealthController(private val healthCheckService: HealthCheckService) {
    @SaCheckRole("admin")
    @GetMapping("/services/health")
    fun getServicesHealth(): Any {
        val healthData = healthCheckService.checkAllServices()
        return healthData
    }
}
