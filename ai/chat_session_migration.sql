-- 为 chat_session 表添加删除标记字段
-- deleted_by_user1: user_id_1 是否删除了会话（拉黑）
-- deleted_by_user2: user_id_2 是否删除了会话（拉黑）

ALTER TABLE chat_session 
ADD COLUMN deleted_by_user1 BOOLEAN DEFAULT FALSE,
ADD COLUMN deleted_by_user2 BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN chat_session.deleted_by_user1 IS 'user_id_1 是否删除了会话（拉黑对方），TRUE表示已删除';
COMMENT ON COLUMN chat_session.deleted_by_user2 IS 'user_id_2 是否删除了会话（拉黑对方），TRUE表示已删除';
