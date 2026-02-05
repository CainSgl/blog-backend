# 聊天功能精简总结

## 已移除的功能和代码

### 1. 数据库字段
- ❌ `chat_message.is_read` - 消息已读状态
- ❌ `chat_session.deleted_by_user1` - 用户1删除标记
- ❌ `chat_session.deleted_by_user2` - 用户2删除标记

### 2. REST API 接口
- ❌ `POST /chat/send` - REST 发送消息（改用 WebSocket）
- ❌ `POST /chat/read/{sessionId}` - 标记消息已读
- ❌ `GET /chat/unread-count` - 获取未读消息数量
- ❌ `DELETE /chat/session/{sessionId}` - 删除会话（拉黑）
- ❌ `POST /chat/session/{sessionId}/restore` - 恢复会话

### 3. Service 方法
- ❌ `markMessagesAsRead()` - 标记消息已读
- ❌ `getUnreadCount()` - 获取未读消息数量
- ❌ `getSessionUnreadCount()` - 获取会话未读数量
- ❌ `deleteSession()` - 删除会话
- ❌ `restoreSession()` - 恢复会话
- ❌ `isSessionBlocked()` - 检查会话是否被拉黑

### 4. Mapper 方法
- ❌ `countUnreadMessages()` - 统计未读消息
- ❌ `markMessagesAsRead()` - 批量标记已读

### 5. WebSocket 消息类型
- ❌ `"read"` 类型消息 - 标记已读通知
- ❌ `handleMarkAsRead()` 方法

### 6. DTO 字段
- ❌ `SendMessageRequest` - 发送消息请求（不再需要 REST 发送）
- ❌ `ChatMessageDTO.isRead` - 消息已读状态

### 7. Redis 相关
- ❌ 未读消息计数功能（`msgMessageCount`）
- ❌ `changeMessageCount()` 调用

## 保留的核心功能

### REST API（仅查询）
✅ `GET /chat/sessions` - 获取会话列表
✅ `POST /chat/messages` - 获取历史消息（分页）
✅ `POST /chat/session/{otherUserId}` - 创建/获取会话

### WebSocket（实时操作）
✅ 发送消息（`type: "message"`）
✅ 接收消息推送
✅ 正在输入状态（`type: "typing"`）

### 数据库表结构
```sql
-- chat_session 表（精简后）
CREATE TABLE chat_session (
    id BIGINT PRIMARY KEY,
    user_id_1 BIGINT NOT NULL,
    user_id_2 BIGINT NOT NULL,
    last_message TEXT,
    last_message_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users UNIQUE (user_id_1, user_id_2)
);

-- chat_message 表（精简后）
CREATE TABLE chat_message (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_message_session
        FOREIGN KEY (session_id) REFERENCES chat_session(id)
        ON DELETE CASCADE
);
```

## 架构设计原则

1. **职责分离**
   - REST API：仅用于查询历史数据
   - WebSocket：处理所有实时操作

2. **简化设计**
   - 移除已读/未读状态追踪
   - 移除拉黑/删除功能
   - 移除未读消息计数

3. **纯粹聊天**
   - 专注于消息的发送和接收
   - 保留正在输入状态提示
   - 保留会话管理

## 迁移步骤

1. 执行数据库迁移脚本：`chat_simplify_migration.sql`
2. 重新编译项目
3. 更新前端代码，移除已读/未读相关逻辑
4. 所有消息发送改用 WebSocket

## 注意事项

- 所有消息发送必须通过 WebSocket
- REST API 仅用于查询历史数据
- 不再追踪消息已读状态
- 不再提供拉黑/删除会话功能
- 接收者离线时消息会保存到数据库，上线后可查询历史消息
