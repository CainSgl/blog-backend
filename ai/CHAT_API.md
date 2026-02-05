# 聊天功能 API 文档

## 概述

聊天系统采用 **REST API + WebSocket** 混合架构：
- **REST API**：用于查询历史数据（会话列表、消息列表）
- **WebSocket**：用于实时消息收发和在线状态通知

## REST API

### 1. 获取会话列表

**请求**
```http
GET /chat/sessions?lastId={lastId}
Headers:
  satoken: YOUR_TOKEN
```

**参数**
- `lastId` (可选): 上次获取的最后一条会话ID
  - 首次请求不传或传 null：返回所有 msg_count > 0 的会话（最多300条）并清零
  - 后续请求传 lastId：正常分页，每次返回20条

**响应**
```json
[
  {
    "id": "123456789",
    "otherUserId": "987654321",
    "lastMessage": "你好，最近怎么样？",
    "lastMessageTime": "2026-02-03T10:30:00",
    "unreadCount": 5
  }
]
```

**字段说明**
- `id`: 会话ID
- `otherUserId`: 对方用户ID
- `lastMessage`: 最后一条消息内容
- `lastMessageTime`: 最后消息时间
- `unreadCount`: 未读消息数

---

### 2. 获取消息列表

**请求**
```http
POST /chat/messages
Headers:
  satoken: YOUR_TOKEN
  Content-Type: application/json
Body:
{
  "sessionId": 123456789,
  "last": "2026-02-03T10:30:00"
}
```

**参数**
- `sessionId` (必填): 会话ID
- `last` (可选): 上次查询最后一条消息的时间戳，首次查询传 null

**响应**
```json
{
  "messages": [
    {
      "id": "111",
      "sessionId": "123456789",
      "senderId": "100",
      "receiverId": "200",
      "content": "你好",
      "createdAt": "2026-02-03T10:30:00"
    }
  ],
  "last": "2026-02-03T10:29:50"
}
```

**字段说明**
- `messages`: 消息列表（按时间倒序，最新的在前），每次最多20条
- `last`: 本次返回的最后一条消息的时间戳，用于下次查询
- 首次查询（`last` 为 null）时，自动清除该会话的未读计数

---

### 3. 创建或获取会话

**请求**
```http
POST /chat/session?otherUserId={otherUserId}
Headers:
  satoken: YOUR_TOKEN
```

**参数**
- `otherUserId` (必填): 对方用户ID

**响应**
```json
{
  "id": 123456789,
  "userId1": 100,
  "userId2": 200,
  "lastMessage": null,
  "lastMessageTime": null,
  "createdAt": "2026-02-03T10:00:00",
  "deletedByUser1": false,
  "deletedByUser2": false,
  "msg1": 0,
  "msg2": 0
}
```

**错误响应**
```json
{
  "code": 403,
  "message": "需要关注对方才能发起会话"
}
```

---

## WebSocket API

### 连接

**WebSocket URL**
```
ws://your-domain/ws/chat?token=YOUR_SATOKEN
```

---

### 发送消息

#### 1. 发送文本消息

**客户端发送**
```json
{
  "type": "message",
  "receiverId": "987654321",
  "content": "你好，最近怎么样？"
}
```

**服务端响应（发送给双方）**
```json
{
  "type": "message",
  "sessionId": "123456789",
  "senderId": "100",
  "receiverId": "987654321",
  "content": "你好，最近怎么样？",
  "messageId": "111",
  "timestamp": "2026-02-03T10:30:00"
}
```

**说明**
- 发送者和接收者都会收到此消息（如果接收者在线）
- 如果接收者不在线，系统会自动增加 Redis 中的未读消息计数
- 只有在双方都未删除会话时才更新 lastMessage 和 lastMessageTime

---

#### 2. 正在输入提示

**客户端发送**
```json
{
  "type": "typing",
  "receiverId": "987654321"
}
```

**服务端推送给接收者**
```json
{
  "type": "typing",
  "senderId": "100",
  "timestamp": "2026-02-03T10:30:00"
}
```

---

## 数据库表结构

### chat_session 表
```sql
CREATE TABLE chat_session (
    id BIGINT PRIMARY KEY,
    user_id_1 BIGINT NOT NULL,
    user_id_2 BIGINT NOT NULL,
    last_message TEXT,
    last_message_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_by_user1 BOOLEAN DEFAULT FALSE,
    deleted_by_user2 BOOLEAN DEFAULT FALSE,
    msg_1 INTEGER DEFAULT 0 NOT NULL,
    msg_2 INTEGER DEFAULT 0 NOT NULL,
    CONSTRAINT uk_users UNIQUE (user_id_1, user_id_2)
);

CREATE INDEX idx_user1_msg ON chat_session(user_id_1, msg_1) WHERE msg_1 > 0;
CREATE INDEX idx_user2_msg ON chat_session(user_id_2, msg_2) WHERE msg_2 > 0;
CREATE INDEX idx_user1_time ON chat_session(user_id_1, last_message_time DESC);
CREATE INDEX idx_user2_time ON chat_session(user_id_2, last_message_time DESC);
```

### chat_message 表
```sql
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

CREATE INDEX idx_session ON chat_message(session_id, created_at DESC);
```

---

## 核心特性

### 1. 会话列表分页逻辑
- **首次请求**（`lastId` 为 null）：返回所有 `msg_count > 0` 的会话（最多300条），并自动清零这些会话的未读计数
- **后续请求**（`lastId` 不为 null）：正常游标分页，每次返回20条

### 2. 未读消息计数
- **msg_1**: user_id_1 的未读消息数
- **msg_2**: user_id_2 的未读消息数
- 发送消息时，如果接收者不在线，调用 `changeMessageCount(1, receiverId)` 增加 Redis 计数
- 首次获取会话列表时，自动清零所有返回的会话的未读计数
- 首次查询消息列表时（`last` 为 null），清除该会话的未读计数

### 3. 会话删除标记
- `deletedByUser1` 和 `deletedByUser2` 标记用户是否删除了会话
- 只有在双方都未删除会话时（都为 false），才会更新 `lastMessage` 和 `lastMessageTime`

### 4. 单向关注创建会话
- 只需要发送方关注接收方即可创建会话
- 如果未关注，返回 403 错误

---

## 错误处理

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| 401 | 未登录或 token 无效 | 重新登录获取 token |
| 403 | 需要关注对方才能发起会话 | 先关注对方用户 |
| 404 | 会话或消息不存在 | 检查 sessionId 是否正确 |
| 500 | 服务器内部错误 | 联系技术支持 |
