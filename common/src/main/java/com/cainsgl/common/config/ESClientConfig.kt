package com.cainsgl.common.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.ElasticsearchTransport
import co.elastic.clients.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.ssl.SSLContexts
import org.elasticsearch.client.RestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StringUtils
import javax.net.ssl.SSLContext
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory

@Configuration
@ConfigurationProperties(prefix = "spring.elasticsearch")
@ConditionalOnClass(ElasticsearchClient::class)
class ESClientConfig
{
    var uris: String = ""
    var username: String = ""
    var password: String = ""

    private val log: Logger = LoggerFactory.getLogger(ESClientConfig::class.java)

    private fun toHttpHost(): Array<HttpHost>
    {
        log.info("===== 调试：注入的spring.elasticsearch.uris = {} =====", uris)
        if (!StringUtils.hasLength(uris))
        {
            throw IllegalArgumentException("spring.elasticsearch.uris 配置不能为空，示例：https://43.163.122.45:32031")
        }
        val uriArray = uris.split(",").toTypedArray()
        val httpHosts = Array(uriArray.size) { i ->
            val uri = uriArray[i].trim()
            try
            {
                HttpHost.create(uri)
            } catch (e: IllegalArgumentException)
            {
                throw IllegalArgumentException(
                    String.format("第%d个ES地址格式错误：%s", i + 1, uri), e
                )
            }
        }
        return httpHosts
    }

    private fun buildSSLContext(): SSLContext?
    {
        try
        {
            val resource = ClassPathResource("es01.crt")
            val factory = CertificateFactory.getInstance("X.509")
            val trustedCa: Certificate
            resource.inputStream.use { inputStream ->
                trustedCa = factory.generateCertificate(inputStream)
            }
            val trustStore = KeyStore.getInstance("pkcs12")
            trustStore.load(null, null)
            trustStore.setCertificateEntry("ca", trustedCa)
            return SSLContexts.custom()
                .loadTrustMaterial(trustStore, null)
                .build()
        } catch (e: Exception)
        {
            log.error("ES SSL证书加载失败（若未开启SSL可忽略）", e)
            return null
        }
    }

    @Bean
    fun elasticsearchClient(): ElasticsearchClient
    {
        try
        {
            val hosts = toHttpHost()

            // 账号密码认证
            val credentialsProvider: CredentialsProvider = BasicCredentialsProvider()
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                UsernamePasswordCredentials(username, password)
            )

            // 构建RestClient
            val builder = RestClient.builder(*hosts)
                .setHttpClientConfigCallback { httpClientBuilder ->
                    // 配置账号密码
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                    // 配置SSL（未开启则注释）
                    val sslContext = buildSSLContext()
                    if (sslContext != null)
                    {
                        httpClientBuilder.setSSLContext(sslContext)
                            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    }
                    httpClientBuilder
                }

            // 创建客户端
            val transport: ElasticsearchTransport = RestClientTransport(
                builder.build(),
                JacksonJsonpMapper()
            )
            return ElasticsearchClient(transport)
        } catch (e: Exception)
        {
            log.error("创建ES客户端失败", e)
            throw RuntimeException("ES客户端初始化失败", e)
        }
    }
}
