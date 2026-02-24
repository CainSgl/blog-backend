# 文章发布异步处理重构说明

## 改动概述
将文章发布后的异步处理逻辑从 `Thread.ofVirtual().start` 改为使用 RocketMQ 消息队列。

## 改动内容

### 1. PostController.kt
- 启用 `RocketMQClientTemplate` 注入
- 创建 `sendPostPublishMessage()` 方法封装MQ消息发送
- 在 `publish()` 方法中，将原来的虚拟线程异步处理改为发送MQ消息

### 2. PostPublishMessage.kt (新增)
消息DTO，包含文章发布所需的所有信息：
- postId: 文章ID
- historyId: 历史版本ID
- userId: 用户ID
- version: 版本号
- content: 内容
- title: 标题
- summary: 摘要
- img: 图片
- tags: 标签列表

### 3. ArticleConsumer.kt
- 新增 `PostPublishMessage` 数据类
- 修改 `ArticlePublishConsumer` 消费者组名为 `article-publish-consumer`
- 消费者处理逻辑：
  1. 更新历史版本
  2. 创建新的历史版本供作者继续编辑
  3. 保存到Elasticsearch
  4. 延时双删缓存
  5. 重新加载向量

### 4. PostHistoryService 接口扩展
新增方法：
- `updateById(historyId: Long, content: String): Boolean`
- `createNewVersion(userId: Long, postId: Long, version: Int, content: String): Boolean`

### 5. PostService 接口扩展
新增方法：
- `saveToElasticsearch(postId: Long, title: String, summary: String?, img: String?, content: String, tags: List<String>?): Boolean`
- `removeCache(postId: Long)`

## 优势
1. **解耦**: 发布流程与后续处理解耦，主流程更快返回
2. **可靠性**: MQ提供消息持久化和重试机制
3. **可观测性**: 可以通过MQ监控消息消费情况
4. **可扩展性**: 可以方便地添加更多消费者处理其他逻辑
5. **容错性**: 消费失败会自动重试，不影响主流程

## 注意事项
- MQ发送失败不影响主流程，只记录日志
- 消费者处理失败会返回 `ConsumeResult.FAILURE` 触发重试
- 保持了原有的延时双删缓存策略
