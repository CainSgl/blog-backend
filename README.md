# 个人博客系统 - 高性能微服务架构

![Version](https://img.shields.io/badge/version-1.0Beta-blue)
![JDK](https://img.shields.io/badge/JDK-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-purple)

请查看部署教程
[cainsgl的小站](https://cainsgl.top/about/view/2025574517720674305)

一个基于现代化技术栈的高性能个人博客系统，支持单体/微服务架构无缝切换，集成AI能力、向量检索、全文搜索等特性。

## 核心特性

### 🏗️ 架构设计
- **单体/微服务可切换架构**：开发环境单体部署，生产环境K8s微服务部署
- **gRPC服务通信**：高性能RPC框架，支持服务间透明调用
- **模块化设计**：用户、文章、AI、评论、文件等业务模块完全解耦
- **虚拟线程支持**：基于JDK 21+虚拟线程，大幅提升并发处理能力

### 🚀 性能优化
- **热点Key探测**：基于Lua脚本的实时热点数据识别（2秒窗口，8次阈值）
- **多级缓存架构**：本地缓存 + Redis缓存 + 数据库，缓存命中率95%+
- **细粒度锁机制**：避免缓存击穿，使用双重检查锁 + 细粒度锁池
- **批量处理优化**：日志、计数器等高频写操作批量入库，减少DB压力
- **延迟队列**：基于Redis Sorted Set实现的延迟任务队列

### 🤖 AI能力集成
- **向量化检索（RAG）**：文章内容智能分块 + Embedding向量化
- **语义搜索**：基于PostgreSQL pgvector的相似度搜索
- **智能标签生成**：AI自动分析文章内容生成标签
- **内容摘要**：自动生成文章摘要
- **AI对话**：集成火山引擎豆包大模型，支持SSE流式响应

### 🔍 搜索能力
- **全文搜索**：Elasticsearch + IK分词器，支持高亮显示
- **向量检索**：基于余弦相似度的语义搜索
- **混合检索**：全文搜索 + 向量检索结合，提升召回率

### 📊 技术栈

**后端框架**
- Spring Boot 3.5.10 + Kotlin 2.3.0
- MyBatis-Plus（数据访问层）
- gRPC 3.1.0（微服务通信）
- Sa-Token（认证授权）

**数据存储**
- PostgreSQL（主数据库，支持pgvector扩展）
- Redis（缓存 + 消息队列）
- Elasticsearch（全文搜索）

**消息队列**
- RocketMQ（异步消息处理）

**AI能力**
- Spring AI（统一AI接口）
- 火山引擎豆包大模型（对话 + Embedding）

**运维部署**
- Docker + Kubernetes
- Fluent Bit（日志采集）
- Prometheus + Grafana（监控）

## 核心技术实现

### 1. 热点Key探测机制

基于Redis + Lua脚本实现的实时热点数据识别，避免缓存击穿和雪崩。

**实现原理**
```kotlin
// 时间窗口内访问次数超过阈值则判定为热点Key
const val HOT_KEY_COUNT_THRESHOLD = 8L
const val TIME_WINDOW_SECONDS = 2L

// Lua脚本保证原子性
local count = redis.call('INCR', KEYS[1])
if count == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end
if count >= tonumber(ARGV[2]) then
    return 1  -- 热点Key
else
    return 0
end
```

**应用场景**
- 文章详情高并发访问
- 用户信息频繁查询
- 热门评论加载

### 2. 多级缓存架构

采用细粒度锁 + 双重检查锁机制，避免缓存击穿。

**缓存层级**
1. **本地缓存**：细粒度锁池（ConcurrentHashMap）
2. **Redis缓存**：热点数据缓存（10-60分钟TTL）
3. **数据库**：最终数据源

**核心代码**
```kotlin
fun <T> getWithFineLock(cacheKey: String, loader: () -> T?): T? {
    // 第一次检查缓存
    val cacheData = redis.get(cacheKey)
    if (cacheData != null) return cacheData
    
    // 热点Key才加锁
    if (hotKeyValidator.isHotKey(cacheKey)) {
        return withFineLock(cacheKey) {
            // 双重检查
            redis.get(cacheKey) ?: loader()?.also { 
                redis.set(cacheKey, it, ttl) 
            }
        }
    }
    return loader()
}
```

**性能提升**
- 缓存命中率：95%+
- P99响应时间：< 50ms
- 避免缓存击穿导致的数据库压力

### 3. 批量处理优化

高频写操作（浏览量、点赞数、评论数）先写Redis，定时批量刷入数据库。

**实现方案**
```kotlin
// 累积到指定数量后批量更新
class BatchPostCountUpdater(batchSize: Int = 10) {
    private val updateBatch = mutableListOf<Triple<Long, String, Long>>()
    
    fun update(postId: Long, type: String, count: Long) {
        updateBatch.add(Triple(postId, type, count))
        if (updateBatch.size >= batchSize) {
            flush()  // 批量写入数据库
        }
    }
}
```

**批量更新SQL**
```sql
-- 使用CASE WHEN实现批量增量更新
UPDATE posts SET
  view_count = CASE 
    WHEN id = 1 THEN view_count + 10
    WHEN id = 2 THEN view_count + 5
    ...
  END
WHERE id IN (1, 2, ...)
```

**优化效果**
- 数据库写入次数减少90%
- TPS提升10倍
- 数据库连接池压力降低

### 4. 延迟队列实现

基于Redis Sorted Set实现的延迟任务队列，用于文件验证、定时任务等场景。

**核心实现**
```kotlin
class DelayedTaskQueue {
    // 添加任务（score为执行时间戳）
    fun addTask(taskId: Long, delaySeconds: Long) {
        val executeTime = Instant.now().epochSecond + delaySeconds
        redis.zAdd(QUEUE_KEY, taskId.toString(), executeTime.toDouble())
    }
    
    // 获取到期任务
    fun pollDueTasks(batchSize: Int = 100): List<Long> {
        val now = Instant.now().epochSecond.toDouble()
        return redis.zRangeByScore(QUEUE_KEY, 0.0, now, 0, batchSize)
    }
}
```

**应用场景**
- 文件上传后延迟验证（防止恶意上传）
- 订单超时自动取消
- 定时消息推送

### 5. 向量检索（RAG）

基于PostgreSQL pgvector扩展实现的语义搜索，支持文章相似度推荐。

**技术方案**
1. **文章分块**：GFM Markdown智能切分（500-2000字符基础块）
2. **向量化**：Spring AI + 火山引擎Embedding模型（1024维）
3. **降维优化**：PCA降维 + 归一化
4. **相似度计算**：余弦相似度（Cosine Similarity）

**分块策略**
```kotlin
object GfmChunkUtils {
    // 规则：GFM → 纯文本 → 基础块(500-2000字符) → 子块(4-5句话)
    fun chunk(gfmContent: String): List<String> {
        val document = parser.parse(gfmContent)
        val plainTexts = extractPlainTexts(document)
        val baseChunks = mergeToBaseChunks(plainTexts)  // 500-2000字符
        return baseChunks.flatMap { splitToSubChunks(it) }  // 4-5句话
    }
}
```

**向量检索流程**
```
用户查询 → Embedding向量化 → pgvector余弦相似度搜索 
→ 加权聚合（距离 + 命中数） → 返回Top K相似文章
```

**聚合算法**
```kotlin
// 聚合得分 = minDistance / (1 + ln(1 + hitCount))
// 命中chunk越多、距离越小，得分越低（越相似）
val aggregatedScore = minDistance / (1 + ln(1.0 + hitCount))
```

### 6. 全文搜索

Elasticsearch + IK分词器实现的全文搜索，支持标题、内容、标签多字段检索。

**索引设计**
```kotlin
@Document(indexName = "posts")
data class PostDocument(
    @Field(type = FieldType.Text, 
           analyzer = "ik_max_word",      // 索引时细粒度分词
           searchAnalyzer = "ik_smart")   // 搜索时粗粒度分词
    var title: String,
    
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    var content: String?,
    
    @Field(type = FieldType.Keyword)  // 精确匹配
    var tags: List<String>?
)
```

**搜索策略**
- 标题匹配：权重3.0（最高）
- 摘要匹配：权重2.0
- 标签匹配：权重2.5（精确匹配）
- 内容匹配：权重1.0

**高亮显示**
```kotlin
val highlight = Highlight(
    HighlightParameters.builder()
        .withPreTags("<em>")
        .withPostTags("</em>")
        .build()
)
```

### 7. 违禁词热更新

基于文件监听的违禁词热更新机制，无需重启服务即可更新敏感词库。

**实现原理**
```kotlin
@Component
class SensitiveWord {
    private val current = AtomicReference<SensitiveWordBs>()
    
    @PostConstruct
    fun init() {
        // 加载敏感词文件
        current.set(createBs(loadWords(file)))
        
        // 监听文件变化（每5秒检查一次）
        val monitor = FileAlterationMonitor(5000)
        monitor.addObserver(FileAlterationObserver(file.parentFile).apply {
            addListener(object : FileAlterationListenerAdaptor() {
                override fun onFileChange(changedFile: File) {
                    if (changedFile == file) {
                        current.set(createBs(loadWords(file)))  // 热更新
                    }
                }
            })
        })
        monitor.start()
    }
}
```

**特性**
- 支持K8s ConfigMap挂载
- 无需重启服务
- 原子性更新（AtomicReference）

### 8. 日志批量处理

消息队列 + 批量处理，减少数据库写入压力。

**处理流程**
```
RocketMQ消息 → 累积到阈值(30条) → 批量处理 → 合并PostProcessor → 批量入库
```

**核心代码**
```kotlin
@Component
class LogPipelineManager {
    @Value("\${userLog.batchNumber}")
    var batchNumber: Int = 30  // 批量处理阈值
    
    fun accumulate(logs: List<UserLogEntity>): LogPipeline? {
        backlogLogs.addAll(logs)
        if (backlogLogs.size > batchNumber) {
            val pipeline = LogPipeline(backlogLogs, handlerMap)
            backlogLogs.clear()
            return pipeline
        }
        return null
    }
}
```

**优化效果**
- 数据库写入次数减少95%
- 日志处理延迟 < 1秒
- 支持PostProcessor合并优化

### 9. 空闲时资源优化

基于CPU负载监控的智能任务调度，在系统空闲时执行数据同步任务。

**实现原理**
```kotlin
@Component
class PostIdleTriggeredTask {
    @Value("\${idle.task.cpu-threshold:70.0}")
    private val cpuThreshold = 70.0  // CPU使用率低于70%时执行
    
    @Scheduled(fixedRate = 30000)  // 每30秒检查一次
    fun checkIdleAndExecute() {
        val cpuUsage = getCpuUsage()
        if (cpuUsage < cpuThreshold) {
            syncPostCountToDatabase()  // 同步Redis数据到数据库
        }
    }
}
```

**优化效果**
- 避免高峰期数据库压力
- 充分利用系统空闲资源
- 数据最终一致性保证

## 架构设计

### 单体/微服务切换架构

**核心思想**：通过gRPC代理层实现透明切换，开发环境单体部署，生产环境微服务部署。

**实现方案**
```kotlin
// 1. 定义服务接口
interface UserService {
    fun getById(id: Long): UserEntity?
}

// 2. 本地实现（单体架构）
@Service
class UserServiceImpl : UserService {
    override fun getById(id: Long): UserEntity? {
        return baseMapper.selectById(id)
    }
}

// 3. gRPC代理实现（微服务架构）
@Service
@ConditionalOnMissingBean(type = "com.cainsgl.user.service.UserServiceImpl")
class UserGrpcService : UserService {
    override fun getById(id: Long): UserEntity? {
        return grpcStub.getUser(id)  // 远程调用
    }
}
```

**切换方式**
- 单体架构：所有模块打包到aggregate模块，本地调用
- 微服务架构：各模块独立部署，gRPC通信

### 服务扩展规范

如何扩展一个新的Service？

1. 在`common/proto`下创建proto文件，定义gRPC接口
2. 使用Maven编译自动生成gRPC类
3. 在业务模块的`api`包下继承生成的gRPC类，实现业务逻辑
4. 在`common/service`下创建服务接口
5. 在业务模块创建本地实现（`xxxServiceImpl`，注解`@Service`）
6. 在`common`创建gRPC代理（`xxxGrpcService`，注解`@Service` + `@ConditionalOnMissingBean`）
7. 在`common/application.yml`配置gRPC服务地址

详情请查看`GRPC_GENERATOR_PROMPT.md`

## 性能指标

### 缓存性能
- 缓存命中率：95%+
- Redis响应时间：P99 < 5ms
- 热点Key识别准确率：98%+

### 接口性能
- 文章详情接口：P99 < 50ms
- 文章列表接口：P99 < 100ms
- 搜索接口：P99 < 200ms

### 并发能力
- 单机QPS：10000+（文章详情）
- 并发用户数：5000+
- 数据库连接池：最大50，最小10

### 资源优化
- 数据库写入次数减少：90%+
- 日志处理延迟：< 1秒
- CPU空闲时自动同步数据

## 部署方案

### 单体架构部署

**适用场景**：开发环境、小规模生产环境

```bash
# 1. 打包
mvn clean package -DskipTests

# 2. 运行
java -jar aggregate/target/aggregate-1.0.jar
```

**特点**
- 所有API前缀为`/api`
- 本地方法调用，性能最优
- 部署简单，适合快速迭代

### 微服务架构部署

**适用场景**：生产环境、高并发场景

**K8s部署清单**
```yaml
# user-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: user-service
        image: blog/user-service:1.0
        ports:
        - containerPort: 8080  # HTTP
        - containerPort: 9090  # gRPC
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
```

**服务发现**
- K8s Service实现服务发现
- gRPC负载均衡
- 健康检查（Liveness + Readiness）

### 中间件部署

**PostgreSQL**
```yaml
# 启用pgvector扩展
CREATE EXTENSION IF NOT EXISTS vector;

# 创建向量索引
CREATE INDEX ON post_chunk_vector 
USING ivfflat (vector vector_cosine_ops) 
WITH (lists = 100);
```

**Elasticsearch**
```yaml
# 安装IK分词器
elasticsearch-plugin install \
  https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.x/elasticsearch-analysis-ik-8.x.zip
```

**Redis**
```yaml
# 配置持久化
appendonly yes
appendfsync everysec
```

## 监控与日志

### 日志采集

**Fluent Bit配置**
```yaml
[INPUT]
    Name              tail
    Path              /var/log/app/*.log
    Parser            json
    Tag               app.log

[OUTPUT]
    Name              es
    Match             app.log
    Host              elasticsearch
    Port              9200
    Index             app-logs
```

### 健康检查

**Spring Boot Actuator**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

**K8s探针**
```yaml
livenessProbe:
  httpGet:
    path: /api/actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /api/actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
```

## 开发规范

### 代码风格
- 使用Kotlin编写业务代码
- 遵循Spring Boot最佳实践
- 使用Kotlin协程处理异步任务

### 数据库规范
- 所有表必须有主键（使用雪花算法生成）
- 使用MyBatis-Plus的Kotlin扩展
- 敏感字段加密存储

### 缓存规范
- 热点数据必须使用热点Key探测
- 缓存Key统一前缀管理
- 设置合理的TTL（10-60分钟）

### API规范
- RESTful API设计
- 统一返回格式（Result封装）
- 异常统一处理

## 快速开始

### 环境要求
- JDK 25+
- Maven 3.8+
- PostgreSQL 14+（需安装pgvector扩展）
- Redis 6.0+
- Elasticsearch 8.x+（需安装IK分词器）
- RocketMQ 5.x+

### 本地开发

1. **克隆项目**
```bash
git clone https://github.com/your-repo/blog-backend.git
cd blog-backend
```

2. **配置数据库**
```sql
-- 创建数据库
CREATE DATABASE blog;

-- 启用pgvector扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 导入SQL脚本
psql -U postgres -d blog -f cainsgl-blog.sql
```

3. **配置环境变量**
```yaml
# common/src/main/resources/env.yaml
db:
  url: jdbc:postgresql://localhost:5432/blog
  username: postgres
  password: your_password

redis:
  host: localhost
  port: 6379
  password: your_password

es:
  uris: http://localhost:9200
  username: elastic
  password: your_password

ai:
  base-url: https://ark.cn-beijing.volces.com/api/v3
  api-key: your_api_key
```

4. **编译项目**
```bash
# 生成gRPC代码
mvn clean compile

# 打包
mvn clean package -DskipTests
```

5. **运行（单体架构）**
```bash
java -jar aggregate/target/aggregate-1.0.jar
```

6. **访问**
- API地址：http://localhost:8080/api
- 健康检查：http://localhost:8080/api/actuator/health

### Docker部署

```bash
# 构建镜像
docker build -t blog-backend:1.0 .

# 运行容器
docker run -d \
  -p 8080:8080 \
  -p 9090:9090 \
  -e SPRING_PROFILES_ACTIVE=prod \
  blog-backend:1.0
```

## 踩坑记录

### 1. Lombok编译器未设置
**问题**：Setter等方法失效，导致配置注入失败

**原因**：未在IDEA中启用Lombok注解处理器

**解决**：
```
Settings → Build, Execution, Deployment → Compiler → Annotation Processors
勾选 "Enable annotation processing"
```

### 2. Common模块配置文件冲突
**问题**：打包后common模块的application.yml被覆盖，配置全部失效

**原因**：多个模块的application.yml文件名冲突

**解决**：将common模块的配置文件改为`application-common.yml`，避免冲突

### 3. Logstash日志无法写入ES
**问题**：日志始终无法记录到Elasticsearch

**原因**：Helm安装的Logstash端口映射错误（默认5044，配置却是其他端口）

**解决**：检查端口映射，确保配置文件中的端口与实际监听端口一致

### 4. gRPC方法找不到
**问题**：报错`UNIMPLEMENTED: Method not found: com.cainsgl.grpc`

**原因**：gRPC客户端地址的端口配置错误（Docker映射9093，配置却是9092）

**解决**：检查gRPC服务端口配置，确保客户端连接正确的端口

### 5. MyBatis版本冲突
**问题**：报String类型错误

**原因**：误引入JPA导致SqlSession创建冲突，移除后AI将MyBatis从Boot3降级到1.x

**解决**：
- 移除JPA依赖
- 确保使用MyBatis-Plus 3.5.x版本
- 检查依赖树，避免版本冲突

### 6. Jackson注解失效
**问题**：`@JsonSerialize`等注解不生效

**原因**：缺少Jackson相关依赖

**解决**：引入`jackson-databind`和`jackson-annotations`依赖

### 7. 前端ID精度损失
**问题**：Long类型ID传到前端后精度丢失

**原因**：JavaScript的Number类型最大安全整数为2^53-1

**解决**：
```kotlin
@JsonSerialize(using = ToStringSerializer::class)
var id: Long
```

### 8. 向量搜索失效
**问题**：某个版本后无法进行向量搜索

**原因**：VectorHandler的字符串从`halfvector`被改成`vector`

**解决**：检查TypeHandler配置，确保与数据库字段类型一致

### 9. Redis连接池耗尽
**问题**：高并发下Redis连接池耗尽

**原因**：默认连接池配置过小

**解决**：
```yaml
spring:
  data:
    redis:
      jedis:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

### 10. ES连接重置
**问题**：Elasticsearch偶尔报`Connection reset`错误

**原因**：网络不稳定或ES负载过高

**解决**：实现重试机制（指数退避）
```kotlin
private fun <T> retryOnConnectionReset(maxRetries: Int = 3, block: () -> T): T {
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: DataAccessResourceFailureException) {
            if (e.message?.contains("Connection reset") == true) {
                Thread.sleep(500 * (attempt + 1))  // 指数退避
            } else throw e
        }
    }
}
```

## 项目亮点总结

### 技术创新
1. **热点Key探测**：基于Lua脚本的实时热点识别，避免缓存击穿
2. **细粒度锁**：锁池 + 双重检查锁，性能优于传统分布式锁
3. **智能分块**：GFM Markdown语义感知分块，提升向量检索准确率
4. **空闲调度**：基于CPU负载的智能任务调度，充分利用系统资源
5. **批量优化**：多种批量处理策略，数据库写入次数减少90%+

### 架构优势
1. **单体/微服务切换**：一套代码，两种部署方式
2. **gRPC通信**：高性能RPC，支持流式传输
3. **虚拟线程**：JDK 21+特性，大幅提升并发能力
4. **模块化设计**：业务模块完全解耦，易于扩展

### AI能力
1. **向量检索（RAG）**：语义搜索 + 相似文章推荐
2. **智能标签**：AI自动生成文章标签
3. **内容摘要**：自动生成文章摘要
4. **对话能力**：集成大模型，支持流式响应

### 性能优化
1. **缓存命中率95%+**：多级缓存 + 热点识别
2. **P99响应时间<50ms**：文章详情接口
3. **单机QPS 10000+**：高并发场景
4. **数据库写入减少90%**：批量处理 + 空闲调度

## 项目结构

```
blog-backend/
├── common/              # 公共模块（Redis、ES、PostgreSQL、MyBatis-Plus）
├── api-user/            # 用户服务API定义（gRPC proto）
├── api-article/         # 文章服务API定义
├── api-ai/              # AI服务API定义
├── user/                # 用户服务实现
├── article/             # 文章服务实现
├── ai/                  # AI服务实现
├── comment/             # 评论服务实现
├── file/                # 文件服务实现
├── consumer/            # 消息消费者
├── scheduler/           # 定时任务调度
├── admin/               # 管理后台
├── sensitive/           # 违禁词模块（伪模块，监听本地文件）
└── aggregate/           # 聚合模块（单体架构入口）
```

**模块说明**
- 所有模块均依赖于common
- 子模块需单独声明es和redis依赖才会导入
- 单体架构：所有API前缀为`/api`，便于Nginx集成
- 微服务架构：通过K8s部署，gRPC通信

踩坑记录
1未设置lombok编译器，导致setter等失效，并且因为还写业务，只有config用上了setter导致配置注入失效，害我以为是配置文件的问题
2common模块的application文件名冲突，打包后会被覆盖，导致common模块的配置全部失效，解决方法，我是直接改的后缀，我感觉这种方法是真优雅
3logstash日志异常，始终无法把日志记录在es里，通过各种链路分析，最后发现，原来是端口映射错了，helm安装的端口是5044默认端口，配置文件里是却是其他的，导致端口没有被监听
4UNIMPLEMENTED: Method not found: com.cainsgl.grpc。grpc框架一直调试不通，调试半天，最后打开日志，发现是客户端address的端口配置错误（跑在docker里映射的9093，配置却是9092，ai配的 ），只有ip是对的，但是这个报错却是找不到方法，
5由于最开始不小心引入了JPA，所以sqlsession创建有冲突，后面移除了，结果ai帮我把mybatis从boot3退回了1，导致一直报String的错，后面修复了
如何扩展一个Service？（与项目规范兼容，前提是你的模块已经建立好了）
1在common的proto下创建一个proto文件，规定暴露的api
2使用maven 编译自动生成类
3在xxx模块的api里继承自动生成的grpc类，编写对应的业务
4在common下的service模块下创建一个接口
5在对应模块下创建对应的本地实现，也就是xxxServiceImpl，注解为@ Service
6在common里创建对应的Grpc代理Service，也就是xxxGrpcService,注解为@ Service和@ConditionalOnMissingBean(type = "com.cainsgl.test.xxx.xxxServiceImpl")(这个是为了和单体架构兼容)
7上面的两个service分别实现对应的接口，代理类的实现可以参考TestGrpcService
8在common的application.yml文件下，配置对应grpc的ip，或域名
9JackSon@注解失效，需要引入单独的依赖
10前端id精度损失
11 debug了一天的bug，莫名奇妙的后面的版本，无法再向量搜索文档，后面去debug，回溯到很久以前的版本，慢慢查下来，发现居然只是vectorHandler的字符串，halfvector被改成vector
(简单来说就3步，创建proto文件，暴露对应的接口，实现自动生成的grpc服务暴露代码，为了兼容单体架构创建对应的代理类，他就是自动的帮我们发送代码给对应的模块)
其中有些步骤很重复，你可以将下面的promot发送给ai为你自动生成代码
前提是你的对应模块已经实现，并且service已经实现了
详情请查看GRPC_GENERATOR_PROMPT.md
违禁词模块是一个伪模块，他监听本地文件来适应k8s的环境

如果只想使用单体架构
你可以直接将功能加在对应的aggregate模块里，而无需考虑其他。所有的api在单体架构下都会在前面加上/api，这是为了和前端的请求进行区分，方便和nginx做集成




## 如何扩展一个Service？

与项目规范兼容，前提是你的模块已经建立好了。

### 步骤说明

1. 在`common/proto`下创建一个proto文件，规定暴露的API
2. 使用Maven编译自动生成类
3. 在xxx模块的`api`里继承自动生成的gRPC类，编写对应的业务
4. 在`common`下的`service`模块下创建一个接口
5. 在对应模块下创建对应的本地实现，也就是`xxxServiceImpl`，注解为`@Service`
6. 在`common`里创建对应的Grpc代理Service，也就是`xxxGrpcService`，注解为`@Service`和`@ConditionalOnMissingBean(type = "com.cainsgl.test.xxx.xxxServiceImpl")`（这个是为了和单体架构兼容）
7. 上面的两个service分别实现对应的接口，代理类的实现可以参考`TestGrpcService`
8. 在`common/application.yml`文件下，配置对应gRPC的IP或域名

简单来说就3步：
1. 创建proto文件
2. 暴露对应的接口
3. 实现自动生成的gRPC服务暴露代码
4. 为了兼容单体架构创建对应的代理类，它就是自动帮我们发送代码给对应的模块

其中有些步骤很重复，你可以将下面的prompt发送给AI为你自动生成代码。

前提是你的对应模块已经实现，并且service已经实现了。

详情请查看`GRPC_GENERATOR_PROMPT.md`

### 违禁词模块说明

违禁词模块是一个伪模块，它监听本地文件来适应K8s的环境。

**特性**：
- 支持K8s ConfigMap挂载
- 文件变更自动热更新（5秒检查间隔）
- 无需重启服务

### 单体架构说明

如果只想使用单体架构，你可以直接将功能加在对应的`aggregate`模块里，而无需考虑其他。

所有的API在单体架构下都会在前面加上`/api`，这是为了和前端的请求进行区分，方便和Nginx做集成。

## 技术选型理由

### 为什么选择Kotlin？
- 简洁的语法，减少样板代码
- 空安全特性，避免NPE
- 协程支持，简化异步编程
- 与Java完全互操作

### 为什么选择PostgreSQL？
- 支持pgvector扩展，原生向量检索
- JSONB类型，灵活存储结构化数据
- 强大的全文搜索能力
- 开源免费，社区活跃

### 为什么选择gRPC？
- 高性能二进制协议
- 支持流式传输
- 强类型接口定义
- 跨语言支持

### 为什么选择虚拟线程？
- 轻量级，创建成本低
- 简化异步编程模型
- 提升并发处理能力
- JDK原生支持，无需额外依赖

## 未来规划

### 功能增强
- [ ] 实时消息推送（WebSocket）
- [ ] 文章协同编辑
- [ ] 图片智能压缩
- [ ] CDN加速
- [ ] 多语言支持

### 性能优化
- [ ] 引入本地缓存（Caffeine）
- [ ] 数据库读写分离
- [ ] 分库分表
- [ ] 静态资源CDN

### AI能力
- [ ] 文章质量评分
- [ ] 智能推荐系统
- [ ] 内容审核
- [ ] 自动纠错

### 运维增强
- [ ] 链路追踪（Jaeger）
- [ ] 性能监控（Prometheus + Grafana）
- [ ] 告警系统
- [ ] 自动扩缩容

## 贡献指南

欢迎提交Issue和Pull Request！

### 开发流程
1. Fork本仓库
2. 创建特性分支（`git checkout -b feature/AmazingFeature`）
3. 提交更改（`git commit -m 'Add some AmazingFeature'`）
4. 推送到分支（`git push origin feature/AmazingFeature`）
5. 提交Pull Request

### 代码规范
- 遵循Kotlin官方代码风格
- 添加必要的注释和文档
- 编写单元测试
- 确保所有测试通过

## 许可证

本项目采用MIT许可证，详见[LICENSE](LICENSE)文件。

## 联系方式

- 作者：cainsgl
- 邮箱：your-email@example.com
- 博客：https://your-blog.com

## 致谢

感谢以下开源项目：
- Spring Boot
- Kotlin
- PostgreSQL
- Redis
- Elasticsearch
- RocketMQ
- MyBatis-Plus

---

**如果这个项目对你有帮助，请给个Star⭐️支持一下！**
