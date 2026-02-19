package com.cainsgl.admin.service

import com.cainsgl.admin.dto.ActuatorHealthResponse
import com.cainsgl.admin.dto.HealthSummary
import com.cainsgl.admin.dto.ServiceHealth
import com.cainsgl.admin.dto.ServiceHealthResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.concurrent.Executors

@Service
class HealthCheckService(private val restClient: RestClient) {

    private val logger = LoggerFactory.getLogger(HealthCheckService::class.java)
    private val executor = Executors.newVirtualThreadPerTaskExecutor()
    //目前写死，admin模块目前都是写死的
    private val services = listOf(
        ServiceConfig("ai-svc", "http://ai-svc:8080"),
        ServiceConfig("comment-svc", "http://comment-svc:8080"),
        ServiceConfig("file-svc", "http://file-svc:8080"),
        ServiceConfig("post-svc", "http://post-svc:8080"),
        ServiceConfig("user-svc", "http://user-svc:8080")
    )

    fun checkAllServices(): ServiceHealthResponse {
        val futures = services.map { service ->
            executor.submit<ServiceHealth> { checkService(service) }
        }

        val results = futures.map { it.get() }
        
        val summary = HealthSummary(
            total = results.size,
            healthy = results.count { it.status == "UP" },
            unhealthy = results.count { it.status == "DOWN" },
            unknown = results.count { it.status == "UNKNOWN" }
        )

        return ServiceHealthResponse(
            services = results,
            summary = summary
        )
    }

    private fun checkService(config: ServiceConfig): ServiceHealth {
        val startTime = System.currentTimeMillis()
        
        return try {
            val response = restClient.get()
                .uri("${config.url}/actuator/health")
                .retrieve()
                .body(ActuatorHealthResponse::class.java)

            val responseTime = System.currentTimeMillis() - startTime
            
            ServiceHealth(
                name = config.name,
                url = config.url,
                status = response?.status ?: "UNKNOWN",
                responseTime = responseTime,
                details = response?.components?.mapValues { (_, component) ->
                    mapOf(
                        "status" to component.status,
                        "details" to (component.details ?: emptyMap())
                    )
                }
            )
        } catch (e: RestClientException) {
            logger.warn("Service ${config.name} health check failed: ${e.message}")
            ServiceHealth(
                name = config.name,
                url = config.url,
                status = "DOWN",
                responseTime = System.currentTimeMillis() - startTime,
                details = mapOf("error" to (e.message ?: "Unknown error"))
            )
        } catch (e: Exception) {
            logger.error("Service ${config.name} health check error", e)
            ServiceHealth(
                name = config.name,
                url = config.url,
                status = "UNKNOWN",
                responseTime = null,
                details = mapOf("error" to (e.message ?: "Connection failed"))
            )
        }
    }

    private data class ServiceConfig(val name: String, val url: String)
}
