package com.cainsgl.admin.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ServiceHealthResponse(
    val services: List<ServiceHealth>,
    val summary: HealthSummary,
    val timestamp: Long = System.currentTimeMillis()
)

data class ServiceHealth(
    val name: String,
    val url: String,
    val status: String,
    val responseTime: Long?,
    val details: Map<String, Any>? = null
)

data class HealthSummary(
    val total: Int,
    val healthy: Int,
    val unhealthy: Int,
    val unknown: Int
)

data class ActuatorHealthResponse(
    val status: String,
    val components: Map<String, ComponentHealth>? = null
)

data class ComponentHealth(
    val status: String,
    val details: Map<String, Any>? = null
)
