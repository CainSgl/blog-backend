package com.cainsgl.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import java.time.Duration

@Configuration
class ElasticsearchConfig : ElasticsearchConfiguration() {

    @Value("\${spring.elasticsearch.uris}")
    private lateinit var uris: String

    @Value("\${spring.elasticsearch.username}")
    private lateinit var username: String

    @Value("\${spring.elasticsearch.password}")
    private lateinit var password: String

    @Value("\${spring.ssl.bundle.pem.es-ssl-bundle.truststore.certificate}")
    private lateinit var certificate: String

    override fun clientConfiguration(): ClientConfiguration {
        return ClientConfiguration.builder()
            .connectedTo(*uris.split(",").toTypedArray())
            .usingSsl()
            .withBasicAuth(username, password)
            .withConnectTimeout(Duration.ofSeconds(10))
            .withSocketTimeout(Duration.ofSeconds(30))
            .build()
    }
}
