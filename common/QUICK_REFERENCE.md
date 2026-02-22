# Fluent Bit 日志快速参考

## 🚀 快速开始

### 无需改动代码！

```kotlin
// 你的代码已经可以正常工作
log.info("用户登录成功")
```

## 📝 推荐写法

### 结构化日志

```kotlin
// ✅ 推荐
log.info("订单创建, userId={}, orderId={}, amount={}", userId, orderId, amount)

// ❌ 不推荐
log.info("用户 ${userId} 创建订单 ${orderId}")
```

### 添加自定义上下文

```kotlin
import org.slf4j.MDC

MDC.put("orderId", orderId)
log.info("处理订单")
MDC.remove("orderId")
```

### 记录异常

```kotlin
try {
    // ...
} catch (e: Exception) {
    log.error("处理失败, orderId={}", orderId, e)
}
```

## 🔧 环境变量

### Kubernetes Deployment

```yaml
env:
  - name: APP_NAME
    value: "blog-user"
  - name: APP_VERSION
    value: "1.0.0"
```

### 本地开发（可选）

```bash
# 使用人类可读格式
export LOG_FORMAT=HumanConsole
```

## 🔍 查询日志

### 查看实时日志

```bash
kubectl logs -f deployment/blog-user --tail=50
```

### 在 Elasticsearch 中查询

```bash
# 查询特定服务
curl -X GET "http://elasticsearch:9200/k8s-logs-*/_search?pretty" \
  -H 'Content-Type: application/json' -d'
{
  "query": { "match": { "app": "blog-user" } },
  "size": 10
}'

# 根据 traceId 追踪
curl -X GET "http://elasticsearch:9200/k8s-logs-*/_search?pretty" \
  -H 'Content-Type: application/json' -d'
{
  "query": { "term": { "trace_id": "your-trace-id" } }
}'

# 查询错误日志
curl -X GET "http://elasticsearch:9200/k8s-logs-*/_search?pretty" \
  -H 'Content-Type: application/json' -d'
{
  "query": { "term": { "log.level": "ERROR" } }
}'
```

## 📊 自动包含的字段

| 字段 | 说明 |
|------|------|
| `trace_id` | 链路追踪 ID |
| `user_id` | 用户 ID（已登录时） |
| `request_path` | 请求路径 |
| `request_method` | 请求方法 |
| `client_ip` | 客户端 IP |
| `app` | 服务名称 |
| `version` | 服务版本 |

## ⚠️ 注意事项

### ❌ 不要做

```kotlin
// 不要输出敏感信息
log.info("password={}", password)

// 不要在循环中打印大量日志
for (item in items) {
    log.debug("item: {}", item)
}

// 不要使用 System.out.println
System.out.println("log message")
```

### ✅ 应该做

```kotlin
// 使用结构化日志
log.info("操作成功, userId={}", userId)

// 记录关键业务操作
log.info("订单支付完成")

// 记录异常时包含上下文
log.error("处理失败, orderId={}", orderId, e)
```

## 📚 详细文档

- [迁移指南](./MIGRATION_GUIDE.md) - 了解变化
- [完整文档](./FLUENT_BIT_LOGGING.md) - 详细说明
- [示例代码](./src/main/java/com/cainsgl/common/util/LoggingExample.kt) - 最佳实践

## 🆘 需要帮助？

- 运维团队：Fluent Bit 和 Elasticsearch 配置
- 架构团队：日志规范和最佳实践
