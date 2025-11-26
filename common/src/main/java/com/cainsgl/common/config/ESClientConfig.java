package com.cainsgl.common.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

@Configuration
@ConfigurationProperties(prefix = "spring.elasticsearch")
@ConditionalOnClass(ElasticsearchClient.class)
public class ESClientConfig {
    public String uris;
    public String username;
    public String password;

    public void setUris(String uris) {
        this.uris = uris;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private Logger log= LoggerFactory.getLogger(ESClientConfig.class);
    private HttpHost[] toHttpHost() {
        log.info("===== 调试：注入的spring.elasticsearch.uris = {} =====", uris);
        if (!StringUtils.hasLength(uris)) {
            throw new IllegalArgumentException("spring.elasticsearch.uris 配置不能为空，示例：https://43.163.122.45:32031");
        }
        String[] uriArray = uris.split(",");
        HttpHost[] httpHosts = new HttpHost[uriArray.length];
        for (int i = 0; i < uriArray.length; i++) {
            String uri = uriArray[i].trim();
            try {
                HttpHost httpHost = HttpHost.create(uri);
                httpHosts[i] = httpHost;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        String.format("第%d个ES地址格式错误：%s", i+1, uri), e
                );
            }
        }
        return httpHosts;
    }

    private SSLContext buildSSLContext() {
        try {
            ClassPathResource resource = new ClassPathResource("es01.crt");
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate trustedCa;
            try (InputStream is = resource.getInputStream()) {
                trustedCa = factory.generateCertificate(is);
            }
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", trustedCa);
            return SSLContexts.custom()
                              .loadTrustMaterial(trustStore, null)
                              .build();
        } catch (Exception e) {
            log.error("ES SSL证书加载失败（若未开启SSL可忽略）", e);
            return null;
        }
    }

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            HttpHost[] hosts = toHttpHost();

            // 账号密码认证
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password)
            );

            // 构建RestClient
            RestClientBuilder builder = RestClient.builder(hosts)
                                                  .setHttpClientConfigCallback(httpClientBuilder -> {
                                                      // 配置账号密码
                                                      httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                                                      // 配置SSL（未开启则注释）
                                                      SSLContext sslContext = buildSSLContext();
                                                      if (sslContext != null) {
                                                          httpClientBuilder.setSSLContext(sslContext)
                                                                           .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                                                      }
                                                      return httpClientBuilder;
                                                  });

            // 创建客户端
            ElasticsearchTransport transport = new RestClientTransport(
                    builder.build(),
                    new JacksonJsonpMapper()
            );
            return new ElasticsearchClient(transport);
        } catch (Exception e) {
            log.error("创建ES客户端失败", e);
            throw new RuntimeException("ES客户端初始化失败", e);
        }
    }
}