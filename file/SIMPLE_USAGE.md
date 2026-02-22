# 文件验证系统 - 简单使用指南

## 核心思路

**只在 Redis 中存储 shortUrl，验证时从数据库查询完整信息**

## Redis 数据结构

```
Key: delayed_task:file_verification
Type: Sorted Set
Member: "123456" (shortUrl)
Score: 1708876800 (Unix 时间戳)
```

## 工作流程

```
1. 用户请求预签名 URL
   ↓
2. 创建文件记录 (status=0)
   ↓
3. Redis 添加: ZADD delayed_task:file_verification 1708876800 "123456"
   ↓
4. 30分钟后，定时任务获取到期的 shortUrl
   ↓
5. 用 shortUrl 从数据库查询文件信息
   ↓
6. 调用 OSS HeadObject 验证文件存在
   ↓
7. 更新数据库 status: 1(可用) 或 2(失败)
   ↓
8. Redis 删除: ZREM delayed_task:file_verification "123456"
```

## 代码示例

### 1. 添加验证任务

```kotlin
// 在 FileController 中
fileVerificationService.addVerificationTask(fileUrlEntity.shortUrl!!)
```

### 2. 定时任务自动验证

```kotlin
// 每分钟自动执行
@Scheduled(cron = "0 * * * * ?")
fun verifyFiles() {
    fileVerificationService.verifyBatch(100)
}
```

### 3. 手动验证单个文件

```kotlin
val success = fileVerificationService.verifyFile(123456L)
```

## 优势

1. **简单**：只存 shortUrl，不需要 JSON 序列化
2. **可靠**：数据库是唯一数据源，不会数据不一致
3. **高效**：字符串操作比 JSON 快
4. **清晰**：代码逻辑一目了然

## Redis 命令示例

```bash
# 查看队列中的任务
ZRANGE delayed_task:file_verification 0 -1 WITHSCORES

# 查看到期的任务（当前时间戳之前）
ZRANGEBYSCORE delayed_task:file_verification 0 1708876800

# 手动删除任务
ZREM delayed_task:file_verification "123456"

# 查看队列大小
ZCARD delayed_task:file_verification
```

## 监控

```kotlin
// 获取待处理任务数
val count = fileVerificationService.getPendingTaskCount()
println("待验证文件数: $count")
```

## 注意事项

1. 确保 Redis 服务正常运行
2. 确保服务器时间准确
3. 定时任务默认每分钟执行一次
4. 每次最多处理 100 个任务
