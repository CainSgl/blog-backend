# Fluent Bit 日志收集适配总结

## 📋 变更概述

已完成从 Logstash 到 Fluent Bit 的日志收集迁移，所有服务的日志将自动以 JSON 格式输出到标准输出，由 Fluent Bit DaemonSet 收集并发送到 Elasticsearch。

## ✅ 已完成的工作

### 1. 日志配置更新

#### 文件：`common/src/main/resources/log4j-spring.xml`

**变更内容：**
- ✅ 移除了 Logstash Socket Appender（不再需要直接连接 Logstash）
- ✅ 配置 Log4j2 使用 ECS (Elastic Common Schema) JSON 格式输出
- ✅ 添加环境变量支持（APP_NAME, APP_VERSION）
- ✅ 支持通过环境变量切换日志格式（JSON/人类可读）

**关键特性：**
```xml
<!-- JSON 格式输出，供 Fluent Bit 收集 -->
<Console name="JsonConsole" target="SYSTEM_OUT">
    <EcsLayout serviceName="${APP_NAME}" serviceVersion="${APP_VERSION}">
        <KeyValuePair key="trace_id" value="%X{traceId}"/>
        <KeyValuePair key="user_id" value="%X{userId}"/>
        <KeyValuePair key="request_path" value="%X{requestPath}"/>
        <!-- ... 更多字段 ... -->
    </EcsLayout>
</Console>
```

### 2. MDC 自动填充拦截器

#### 新文件：`common/src/main/java/com/cainsgl/common/config/interceptor/LoggingMdcInterceptor.kt`

**功能：**
- ✅ 自动为每个 HTTP 请求添加日志上下文
- ✅ 从 OpenTelemetry 获取 traceId 和 spanId
- ✅ 从 Sa-Token 获取 userId（已登录时）
- ✅ 记录请求路径、方法和客户端 IP
- ✅ 请求结束后自动清理 MDC，避免内存泄漏

**自动包含的字段：**
- `traceId`: 链路追踪 ID
- `spanId`: Span ID
- `userId`: 用户 ID
- `requestPath`: 请求路径
- `requestMethod`: 请求方法（GET, POST 等）
- `clientIp`: 客户端真实 IP（考虑代理）

### 3. WebMVC 配置更新

#### 文件：`common/src/main/java/com/cainsgl/common/config/CainsglWebMvcConfig.kt`

**变更内容：**
- ✅ 注册 `LoggingMdcInterceptor` 到拦截器链
- ✅ 排除健康检查端点（/actuator/**）
- ✅ 排除错误页面（/error）

### 4. 文档和示例

#### 新增文档：

1. **`common/MIGRATION_GUIDE.md`** - 迁移指南
   - 快速了解变化
   - 开发人员需要做什么
   - Kubernetes 配置示例
   - 常见问题解答

2. **`common/FLUENT_BIT_LOGGING.md`** - 完整使用文档
   - 详细的配置说明
   - 日志查询示例
   - 最佳实践
   - 性能优化建议
   - 故障排查指南

3. **`common/README.md`** - 模块说明
   - 快速开始指南
   - 工具类说明
   - 配置说明

#### 新增示例代码：

1. **`common/src/main/java/com/cainsgl/common/util/LoggingExample.kt`**
   - 基本日志记录示例
   - 结构化日志示例
   - 异常日志示例
   - 自定义 MDC 上下文示例
   - 错误示例（不要这样做）

2. **`common/src/test/java/com/cainsgl/common/logging/LoggingTest.kt`**
   - 日志功能测试
   - 用于验证配置是否正确

## 📊 日志格式对比

### 之前（Logstash）

```
[trace-id-123]2024-01-15 10:30:00.000  INFO --- [nio-8080-exec-1] c.c.user.service.UserService : 用户登录成功
```

### 现在（Fluent Bit + JSON）

```json
{
  "@timestamp": "2024-01-15T10:30:00.000Z",
  "log.level": "INFO",
  "message": "用户登录成功",
  "app": "blog-user",
  "version": "1.0.0",
  "trace_id": "trace-id-123",
  "span_id": "span-id-456",
  "user_id": "12345",
  "request_path": "/api/user/login",
  "request_method": "POST",
  "client_ip": "192.168.1.100",
  "thread_name": "http-nio-8080-exec-1",
  "logger_name": "com.cainsgl.user.service.UserService",
  "kubernetes": {
    "pod_name": "blog-user-7d8f9c5b6-abc12",
    "namespace_name": "default",
    "labels": {
      "app": "blog-user",
      "version": "v1.0"
    }
  }
}
```

## 🚀 部署要求

### Kubernetes Deployment 配置

每个服务的 Deployment 需要添加以下配置：

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

## 📝 开发人员指南

### 对于大多数开发人员：无需任何改动！

你的代码已经在使用 SLF4J 记录日志，这些日志会自动以 JSON 格式输出。

```kotlin
// 这样的代码无需修改，会自动工作
log.info("用户登录成功")
log.error("数据库连接失败", exception)
```

### 推荐的优化（可选）

#### 1. 使用结构化日志

```kotlin
// ✅ 推荐
log.info("订单创建成功, userId={}, orderId={}, amount={}", userId, orderId, amount)

// ❌ 不推荐
log.info("用户 ${userId} 创建了订单 ${orderId}，金额 ${amount}")
```

#### 2. 添加自定义业务上下文

```kotlin
import org.slf4j.MDC

MDC.put("orderId", orderId)
log.info("开始处理订单")
// ... 业务逻辑 ...
log.info("订单处理完成")
MDC.remove("orderId")
```

## 🔍 日志查询示例

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

### 根据 traceId 追踪完整请求链路

```bash
curl -X GET "http://elasticsearch:9200/k8s-logs-*/_search?pretty" \
  -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": { "trace_id": "your-trace-id" }
  },
  "sort": [{ "@timestamp": "asc" }]
}'
```

### 查询特定用户的操作日志

```bash
curl -X GET "http://elasticsearch:9200/k8s-logs-*/_search?pretty" \
  -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": { "user_id": "123456" }
  }
}'
```

## 🧪 测试验证

### 1. 本地测试

```bash
# 使用 JSON 格式
cd common
mvn test -Dtest=LoggingTest

# 使用人类可读格式
export LOG_FORMAT=HumanConsole
mvn test -Dtest=LoggingTest
```

### 2. 部署后验证

```bash
# 1. 检查 Pod 日志是否为 JSON 格式
kubectl logs -f deployment/blog-user --tail=10

# 2. 检查 Fluent Bit 是否正常运行
kubectl get pods -n kube-system -l app=fluent-bit
kubectl logs -n kube-system -l app=fluent-bit --tail=50

# 3. 检查 Elasticsearch 索引
kubectl exec -it deployment/elasticsearch -- curl -X GET "localhost:9200/_cat/indices?v"

# 4. 查询日志
kubectl exec -it deployment/elasticsearch -- curl -X GET "localhost:9200/k8s-logs-*/_search?pretty&size=5"
```

## 📋 检查清单

### Common 模块
- [x] 更新 `log4j-spring.xml` 配置
- [x] 创建 `LoggingMdcInterceptor` 拦截器
- [x] 更新 `CainsglWebMvcConfig` 注册拦截器
- [x] 创建迁移指南文档
- [x] 创建完整使用文档
- [x] 创建示例代码
- [x] 创建测试用例

### 各服务模块
- [ ] 在 Deployment YAML 中添加 `APP_NAME` 环境变量
- [ ] 在 Deployment YAML 中添加 `APP_VERSION` 环境变量
- [ ] 在 Pod 模板中添加 `fluentbit.io/parser: json` 注解
- [ ] 测试日志是否正常输出到 Elasticsearch
- [ ] 验证日志字段是否完整

### 运维配置
- [ ] 确认 Fluent Bit DaemonSet 已部署
- [ ] 确认 Elasticsearch 正常运行
- [ ] 配置 Elasticsearch 索引生命周期管理（ILM）
- [ ] 配置 Kibana 索引模式（如果使用）
- [ ] 设置日志告警规则

## 🎯 优势

### 相比 Logstash 的优势

1. **更轻量级**：Fluent Bit 内存占用更小（~650KB vs ~500MB）
2. **更高性能**：C 语言编写，性能更好
3. **更简单**：无需应用直接连接 Logstash，只需输出到 stdout
4. **更灵活**：DaemonSet 部署，自动收集所有容器日志
5. **更可靠**：应用不依赖日志收集器，解耦更好

### JSON 格式的优势

1. **结构化**：Elasticsearch 可以更好地索引和查询
2. **标准化**：使用 ECS 标准，与 Elastic Stack 完美集成
3. **丰富**：自动包含大量元数据（Kubernetes 信息、链路追踪等）
4. **可扩展**：易于添加自定义字段

## 📚 相关文档

- [迁移指南](./common/MIGRATION_GUIDE.md)
- [完整使用文档](./common/FLUENT_BIT_LOGGING.md)
- [Common 模块说明](./common/README.md)
- [日志示例代码](./common/src/main/java/com/cainsgl/common/util/LoggingExample.kt)

## 🆘 支持

如果遇到问题，请联系：
- **运维团队**：Fluent Bit 和 Elasticsearch 配置问题
- **架构团队**：日志规范和最佳实践问题

---

**完成日期：** 2024-01-15  
**维护者：** 架构团队  
**版本：** 1.0.0
