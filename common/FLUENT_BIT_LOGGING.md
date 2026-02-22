# Fluent Bit 日志收集适配说明

## 概述

本项目已完成 Fluent Bit 日志收集适配，所有服务的日志将自动以 JSON 格式输出到标准输出（stdout），由 Fluent Bit DaemonSet 自动收集并发送到 Elasticsearch。

## 已完成的配置

### 1. Log4j2 JSON 格式输出

已将 `common/src/main/resources/log4j-spring.xml` 配置为使用 ECS (Elastic Common Schema) JSON 格式输出日志。

**自动包含的字段：**
- `@timestamp`: 日志时间戳
- `log.level`: 日志级别（INFO, WARN, ERROR 等）
- `message`: 日志消息
- `service.name`: 服务名称（从环境变量 `APP_NAME` 读取）
- `service.version`: 服务版本（从环境变量 `APP_VERSION` 读取）
- `trace_id`: 链路追踪 ID（从 MDC 或 OpenTelemetry 获取）
- `span_id`: Span ID（从 OpenTelemetry 获取）
- `user_id`: 用户 ID（从 Sa-Token 获取）
- `request_path`: 请求路径
- `request_method`: 请求方法（GET, POST 等）
- `client_ip`: 客户端 IP 地址
- `thread_name`: 线程名称
- `logger_name`: Logger 名称

### 2. MDC 自动填充

已创建 `LoggingMdcInterceptor` 拦截器，自动为每个 HTTP 请求添加以下上下文信息：
- `traceId`: 链路追踪 ID（优先从 OpenTelemetry 获取，否则自动生成）
- `spanId`: Span ID（从 OpenTelemetry 获取）
- `userId`: 用户 ID（从 Sa-Token 获取，未登录时不设置）
- `requestPath`: 请求路径
- `requestMethod`: 请求方法
- `clientIp`: 客户端真实 IP（考虑代理和负载均衡）

### 3. gRPC 请求支持

已有的 `TraceIdInterceptor` 会自动为 gRPC 请求设置 `traceId`。

## 使用方式

### 基本日志记录

```kotlin
import org.slf4j.LoggerFactory

class YourService {
    private val log = LoggerFactory.getLogger(javaClass)

    fun yourMethod() {
        // 自动包含 traceId, userId, requestPath 等上下文信息
        log.info("用户操作成功")
        log.warn("缓存未命中，使用数据库查询")
        log.error("数据库连接失败", exception)
        log.debug("查询参数: {}", params)
    }
}
```

### 添加自定义上下文

```kotlin
import org.slf4j.MDC

// 在需要的地方添加自定义上下文
MDC.put("orderId", order.id.toString())
MDC.put("paymentMethod", "alipay")

log.info("订单支付成功")

// 使用完后清理（可选，请求结束时会自动清理）
MDC.remove("orderId")
MDC.remove("paymentMethod")
```

### 结构化日志（推荐）

```kotlin
// 使用占位符，避免字符串拼接
log.info("用户登录成功, userId={}, loginTime={}", userId, loginTime)

// 记录异常堆栈
try {
    // ...
} catch (e: Exception) {
    log.error("处理订单失败, orderId={}", orderId, e)
}
```

## 环境变量配置

### 必需的环境变量

在 Kubernetes Deployment 中需要设置以下环境变量：

```yaml
env:
  - name: APP_NAME
    value: "blog-user"  # 服务名称
  - name: APP_VERSION
    value: "1.0.0"      # 服务版本
```

### 可选的环境变量

```yaml
env:
  # 切换日志格式（默认为 JSON）
  - name: LOG_FORMAT
    value: "JsonConsole"  # 或 "HumanConsole" 用于本地开发
```

## 本地开发

### 使用人类可读格式

在本地开发时，可以使用人类可读的日志格式：

```yaml
# application-dev.yml 或环境变量
LOG_FORMAT: HumanConsole
```

或者在启动时设置：

```bash
export LOG_FORMAT=HumanConsole
mvn spring-boot:run
```

### 使用 JSON 格式（推荐）

即使在本地开发，也建议使用 JSON 格式，可以使用 `jq` 工具美化输出：

```bash
mvn spring-boot:run | jq -R 'fromjson? | .'
```

## 在 Elasticsearch 中查询日志

### 查询特定服务的日志

```bash
curl -X GET "http://elasticsearch:9200/k8s-logs-*/_search?pretty" \
  -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        { "match": { "app": "blog-user" } },
        { "range": { "@timestamp": { "gte": "now-1h" } } }
      ]
    }
  },
  "sort": [{ "@timestamp": "desc" }],
  "size": 100
}'
```

### 查询特定用户的操作日志

```bash
curl -X GET "http://elasticsearch:9200/k8s-logs-*/_search?pretty" \
  -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        { "term": { "user_id": "123456" } },
        { "range": { "@timestamp": { "gte": "now-24h" } } }
      ]
    }
  },
  "sort": [{ "@timestamp": "desc" }]
}'
```

### 查询错误日志

```bash
curl -X GET "http://elasticsearch:9200/k8s-logs-*/_search?pretty" \
  -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        { "term": { "log.level": "ERROR" } },
        { "range": { "@timestamp": { "gte": "now-1h" } } }
      ]
    }
  },
  "sort": [{ "@timestamp": "desc" }]
}'
```

### 根据 traceId 追踪完整请求链路

```bash
curl -X GET "http://elasticsearch:9200/k8s-logs-*/_search?pretty" \
  -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": { "trace_id": "your-trace-id-here" }
  },
  "sort": [{ "@timestamp": "asc" }]
}'
```

## 日志级别配置

### 全局日志级别

在 `application.yml` 中配置：

```yaml
logging:
  level:
    root: INFO
    com.cainsgl: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
```

### 动态调整日志级别（通过 Actuator）

```bash
# 查看当前日志级别
curl http://localhost:8080/actuator/loggers/com.cainsgl.user

# 动态修改日志级别
curl -X POST http://localhost:8080/actuator/loggers/com.cainsgl.user \
  -H 'Content-Type: application/json' \
  -d '{"configuredLevel": "DEBUG"}'
```

## 最佳实践

### ✅ 推荐做法

```kotlin
// 1. 使用结构化日志
log.info("订单创建成功, orderId={}, userId={}, amount={}", orderId, userId, amount)

// 2. 记录关键业务操作
log.info("用户登录成功")
log.info("订单支付完成")
log.info("文章发布成功")

// 3. 记录异常时包含上下文
log.error("支付失败, orderId={}, paymentMethod={}", orderId, method, exception)

// 4. 使用合适的日志级别
log.debug("查询参数: {}", params)  // 调试信息
log.info("用户操作成功")           // 正常业务流程
log.warn("缓存未命中")             // 警告但不影响功能
log.error("数据库连接失败", e)     // 错误需要关注
```

### ❌ 避免的做法

```kotlin
// 1. 不要使用 System.out.println
System.out.println("用户登录")  // ❌

// 2. 不要在日志中输出敏感信息
log.info("用户登录: password={}", password)  // ❌

// 3. 不要在循环中打印大量日志
for (item in items) {
    log.debug("处理: {}", item)  // ❌ 可能产生大量日志
}

// 4. 不要使用字符串拼接
log.info("用户 " + userId + " 登录成功")  // ❌ 性能差
```

## 性能优化

### 使用异步日志（可选）

如果日志量很大，可以启用异步日志：

```xml
<!-- log4j-spring.xml -->
<Appenders>
    <Async name="AsyncJsonConsole">
        <AppenderRef ref="JsonConsole"/>
        <BlockingQueueFactory class="org.apache.logging.log4j.core.async.ArrayBlockingQueueFactory">
            <Size>1024</Size>
        </BlockingQueueFactory>
    </Async>
</Appenders>

<Loggers>
    <Root level="INFO">
        <AppenderRef ref="AsyncJsonConsole"/>
    </Root>
</Loggers>
```

## 故障排查

### 日志没有出现在 Elasticsearch

1. 检查 Pod 是否正常输出日志：
   ```bash
   kubectl logs -f deployment/blog-user --tail=50
   ```

2. 检查 Fluent Bit 是否正常运行：
   ```bash
   kubectl get pods -n kube-system -l app=fluent-bit
   kubectl logs -n kube-system -l app=fluent-bit --tail=50
   ```

3. 检查 Elasticsearch 索引：
   ```bash
   kubectl exec -it deployment/elasticsearch -- curl -X GET "localhost:9200/_cat/indices?v"
   ```

### 日志格式不正确

确保环境变量 `APP_NAME` 和 `APP_VERSION` 已正确设置。

### MDC 信息丢失

确保 `LoggingMdcInterceptor` 已正确注册到 `CainsglWebMvcConfig` 中。

## 迁移检查清单

- [x] 移除 Logstash Socket Appender
- [x] 配置 Log4j2 JSON 格式输出
- [x] 创建 MDC 拦截器自动填充上下文
- [x] 注册拦截器到 WebMVC 配置
- [x] 在 Deployment 中添加环境变量 `APP_NAME` 和 `APP_VERSION`
- [ ] 测试日志是否正常输出到 Elasticsearch
- [ ] 在 Kibana 中创建索引模式和仪表板（如果使用）

## 相关资源

- [Fluent Bit 官方文档](https://docs.fluentbit.io/)
- [ECS Logging 规范](https://www.elastic.co/guide/en/ecs-logging/overview/current/intro.html)
- [Log4j2 ECS Layout](https://www.elastic.co/guide/en/ecs-logging/java/current/setup.html)
- [Elasticsearch Query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)
