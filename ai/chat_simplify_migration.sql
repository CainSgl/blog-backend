-- 聊天功能精简迁移脚本
-- 移除不必要的字段：is_read, deleted_by_user1, deleted_by_user2

-- 1. 移除 chat_message 表的 is_read 字段
ALTER TABLE chat_message DROP COLUMN IF EXISTS is_read;

-- 2. 移除 chat_session 表的拉黑相关字段
ALTER TABLE chat_session DROP COLUMN IF EXISTS deleted_by_user1;
ALTER TABLE chat_session DROP COLUMN IF EXISTS deleted_by_user2;

-- 完成！聊天功能已精简为纯粹的消息传递
