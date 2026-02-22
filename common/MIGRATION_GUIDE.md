# Fluent Bit 日志迁移指南

## 🎯 变更概述

运维团队已将 Logstash 替换为 Fluent Bit，用于日志收集。本次迁移对开发人员几乎透明，无需修改业务代码。

## ✅ 已完成的工作

### 1. 日志格式升级
- ✅ 移除了 Logstash Socket Appender
- ✅ 配置 Log4j2 输出 JSON 格式日志（ECS 标准）
- ✅ 日志自动包含丰富的元数据（traceId, userId, requestPath 等）

### 2. 自动上下文填充
- ✅ 创建 `LoggingMdcInterceptor` 自动为每个请求添加上下文
- ✅ 支持从 OpenTelemetry 自动获取 traceId 和 spanId
- ✅ 支持从 Sa-Token 自动获取 userId
- ✅ 自动记录请求路径、方法和客户端 IP

### 3. 配置更新
- ✅ 更新 `log4j-spring.xml` 配置文件
- ✅ 注册 MDC 拦截器到 WebMVC 配置
- ✅ 支持环境变量切换日志格式（JSON/人类可读）

## 📋 开发人员需要做什么？

### 对于大多数开发人员：无需任何改动！

你的代码已经在使用 SLF4J 记录日志，这些日志会自动以 JSON 格式输出，并被 Fluent Bit 收集。

```kotlin
// 这样的代码无需修改，会自动工作
log.info("用户登录成功")
log.error("数据库连接失败", exception)
```

### 可选优化（推荐）

#### 1. 使用结构化日志

```kotlin
// ❌ 之前可能这样写
log.info("用户 ${userId} 创建了订单 ${orderId}")

// ✅ 推荐改为结构化日志
log.info("用户创建订单成功, userId={}, orderId={}", userId, orderId)
```

**好处：** Elasticsearch 可以更好地索引和查询这些字段。

#### 2. 添加自定义业务上下文

```kotlin
import org.slf4j.MDC

fun processOrder(orderId: String) {
    try {
        MDC.put("orderId", orderId)
        log.info("开始处理订单")
        // ... 业务逻辑 ...
        log.info("订单处理完成")
    } finally {
        MDC.remove("orderId")
    }
}
```

**好处：** 可以在 Elasticsearch 中按 orderId 查询所有相关日志。

## 🚀 Kubernetes 部署配置

### 必需的环境变量

在你的服务的 Deployment YAML 中添加：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: blog-user
spec:
  template:
    metadata:
      labels:
        app: blog-user
        version: v1.0
      annotations:
        fluentbit.io/parser: json  # 告诉 Fluent Bit 使用 JSON 解析器
    spec:
      containers:
      - name: blog-user
        image: your-image:tag
        env:
          # 必需：服务名称和版本
          - name: APP_NAME
            value: "blog-user"
          - name: APP_VERSION
            value: "1.0.0"
          # 可选：本地开发时使用人类可读格式
          # - name: LOG_FORMAT
          #   value: "HumanConsole"
```

## 🔍 查询日志

### 在 Elasticsearch 中查询

```bash
# 查询特定服务的日志
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

# 查询特定用户的操作
curl -X GET "http://elasticsearch:9200/k8s-logs-*/_search?pretty" \
  -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": { "user_id": "123456" }
  }
}'

# 根据 traceId 追踪完整请求链路
curl -X GET "http://elasticsearch:9200/k8s-logs-*/_search?pretty" \
  -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": { "trace_id": "your-trace-id" }
  },
  "sort": [{ "@timestamp": "asc" }]
}'
```

## 🧪 本地测试

### 方式 1：使用 JSON 格式（推荐）

```bash
# 启动应用，日志会以 JSON 格式输出
mvn spring-boot:run

# 使用 jq 美化输出
mvn spring-boot:run | jq -R 'fromjson? | .'
```

### 方式 2：使用人类可读格式

```bash
# 设置环境变量
export LOG_FORMAT=HumanConsole
mvn spring-boot:run
```

或在 `application-dev.yml` 中添加：

```yaml
# 仅用于本地开发
LOG_FORMAT: HumanConsole
```

## 📊 日志字段说明

每条日志会自动包含以下字段：

| 字段 | 说明 | 来源 |
|------|------|------|
| `@timestamp` | 日志时间戳 | 自动生成 |
| `log.level` | 日志级别 | INFO/WARN/ERROR/DEBUG |
| `message` | 日志消息 | 你的代码 |
| `app` | 服务名称 | 环境变量 `APP_NAME` |
| `version` | 服务版本 | 环境变量 `APP_VERSION` |
| `trace_id` | 链路追踪 ID | OpenTelemetry 或自动生成 |
| `span_id` | Span ID | OpenTelemetry |
| `user_id` | 用户 ID | Sa-Token（已登录时） |
| `request_path` | 请求路径 | HTTP 请求 |
| `request_method` | 请求方法 | HTTP 请求 |
| `client_ip` | 客户端 IP | HTTP 请求头 |
| `thread_name` | 线程名称 | 自动获取 |
| `logger_name` | Logger 名称 | 类名 |

## ⚠️ 注意事项

### 1. 不要在日志中输出敏感信息

```kotlin
// ❌ 错误
log.info("用户登录: password={}", password)
log.info("API 密钥: {}", apiKey)

// ✅ 正确
log.info("用户登录成功, userId={}", userId)
```

### 2. 避免在循环中打印大量日志

```kotlin
// ❌ 可能产生大量日志
for (item in items) {
    log.debug("处理项目: {}", item)
}

// ✅ 只记录关键信息
log.info("开始批量处理, 总数={}", items.size)
// ... 处理逻辑 ...
log.info("批量处理完成, 成功={}, 失败={}", successCount, failCount)
```

### 3. 使用合适的日志级别

- `DEBUG`: 详细的调试信息（生产环境不输出）
- `INFO`: 正常的业务流程信息
- `WARN`: 警告信息，不影响功能但需要关注
- `ERROR`: 错误信息，需要立即处理

## 📚 参考资料

- [详细使用文档](./FLUENT_BIT_LOGGING.md)
- [日志记录示例代码](../src/main/java/com/cainsgl/common/util/LoggingExample.kt)
- [Fluent Bit 官方文档](https://docs.fluentbit.io/)
- [ECS Logging 规范](https://www.elastic.co/guide/en/ecs-logging/overview/current/intro.html)

## ❓ 常见问题

### Q: 我需要修改代码吗？
A: 大多数情况下不需要。只要你使用 SLF4J 记录日志，就会自动工作。

### Q: 日志会丢失吗？
A: Fluent Bit 有重试机制，正常情况下不会丢失。

### Q: 如何查看我的日志？
A: 可以通过 `kubectl logs` 查看实时日志，或在 Elasticsearch/Kibana 中查询历史日志。

### Q: 本地开发时日志格式太难看？
A: 设置环境变量 `LOG_FORMAT=HumanConsole` 即可使用人类可读格式。

### Q: 如何追踪一个完整的请求链路？
A: 使用日志中的 `trace_id` 字段在 Elasticsearch 中查询所有相关日志。

## 🆘 需要帮助？

如果遇到问题，请联系：
- 运维团队：负责 Fluent Bit 和 Elasticsearch 配置
- 架构团队：负责日志规范和最佳实践

---

**最后更新：** 2024-01-15  
**维护者：** 架构团队
