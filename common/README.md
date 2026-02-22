# Common 模块

通用工具模块，封装工具类、通用异常、DTO、配置等。

## 📋 目录

- [日志配置](#日志配置)
- [配置说明](#配置说明)
- [工具类](#工具类)
- [异常处理](#异常处理)

## 📝 日志配置

### Fluent Bit 日志收集

本项目已完成 Fluent Bit 日志收集适配，所有日志自动以 JSON 格式输出并被收集到 Elasticsearch。

#### 快速开始

**对于大多数开发人员：无需任何改动！**

```kotlin
// 你的代码已经可以正常工作
import org.slf4j.LoggerFactory

class YourService {
    private val log = LoggerFactory.getLogger(javaClass)
    
    fun yourMethod() {
        log.info("用户操作成功")  // 自动包含 traceId, userId 等上下文
    }
}
```

#### 日志自动包含的信息

每条日志会自动包含：
- `trace_id`: 链路追踪 ID（从 OpenTelemetry 或自动生成）
- `user_id`: 用户 ID（从 Sa-Token 获取，已登录时）
- `request_path`: 请求路径
- `request_method`: 请求方法
- `client_ip`: 客户端 IP
- `app`: 服务名称
- `version`: 服务版本

#### 推荐的日志写法

```kotlin
// ✅ 使用结构化日志
log.info("订单创建成功, userId={}, orderId={}, amount={}", userId, orderId, amount)

// ✅ 记录异常时包含上下文
log.error("支付失败, orderId={}", orderId, exception)

// ✅ 添加自定义业务上下文
MDC.put("orderId", orderId)
log.info("开始处理订单")
MDC.remove("orderId")
```

#### 详细文档

- [迁移指南](./MIGRATION_GUIDE.md) - 快速了解变化和需要做什么
- [完整使用文档](./FLUENT_BIT_LOGGING.md) - 详细的配置和使用说明
- [示例代码](./src/main/java/com/cainsgl/common/util/LoggingExample.kt) - 日志记录最佳实践

### 本地开发

#### 使用人类可读格式（推荐本地开发）

```bash
# 方式 1：环境变量
export LOG_FORMAT=HumanConsole
mvn spring-boot:run

# 方式 2：在 application-dev.yml 中配置
# LOG_FORMAT: HumanConsole
```

#### 使用 JSON 格式（推荐生产环境）

```bash
# 默认就是 JSON 格式
mvn spring-boot:run

# 使用 jq 美化输出
mvn spring-boot:run | jq -R 'fromjson? | .'
```

## ⚙️ 配置说明

### 必需的环境变量

在 Kubernetes Deployment 中需要设置：

```yaml
env:
  - name: APP_NAME
    value: "your-service-name"  # 服务名称
  - name: APP_VERSION
    value: "1.0.0"              # 服务版本
```

### 可选的环境变量

```yaml
env:
  - name: LOG_FORMAT
    value: "JsonConsole"  # 或 "HumanConsole"
```

## 🔧 工具类

### TraceIdUtils

获取当前请求的 traceId：

```kotlin
import com.cainsgl.common.util.TraceIdUtils

val traceId = TraceIdUtils.getTraceId()
```

### 其他工具类

- `FineLockCacheUtils`: 细粒度锁缓存工具
- `HotKeyValidator`: 热点 Key 验证
- `MqUtils`: 消息队列工具
- `VectorUtils`: 向量计算工具

## 🚨 异常处理

### BusinessException

业务异常，用于业务逻辑错误：

```kotlin
import com.cainsgl.common.exception.BusinessException

throw BusinessException("用户不存在")
```

### BSystemException

系统异常，用于系统级错误：

```kotlin
import com.cainsgl.common.exception.BSystemException

throw BSystemException("数据库连接失败")
```

## 📦 依赖说明

本模块包含以下主要依赖：

- Spring Boot Web
- Spring Boot Actuator（健康检查）
- MyBatis Plus
- Redis & Redisson
- RocketMQ
- Elasticsearch
- Sa-Token（认证授权）
- Log4j2 + ECS Layout（日志）
- gRPC
- OpenTelemetry（链路追踪）

## 🧪 测试

运行日志测试：

```bash
cd common
mvn test -Dtest=LoggingTest
```

## 📚 相关文档

- [Fluent Bit 迁移指南](./MIGRATION_GUIDE.md)
- [Fluent Bit 完整文档](./FLUENT_BIT_LOGGING.md)
- [日志示例代码](./src/main/java/com/cainsgl/common/util/LoggingExample.kt)

## 🆘 需要帮助？

如果遇到问题，请联系：
- 运维团队：Fluent Bit 和 Elasticsearch 配置
- 架构团队：日志规范和最佳实践
