package com.cainsgl.ai.controller

import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.ai.dto.request.GetMessagesRequest
import com.cainsgl.ai.dto.response.ChatSessionDTO
import com.cainsgl.ai.entity.ChatSessionEntity
import com.cainsgl.ai.service.ChatServiceImpl
import com.cainsgl.ai.service.ChatSessionImpl
import com.cainsgl.common.dto.response.ResultCode
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/chat")
class ChatController {

    @Resource
    lateinit var chatService: ChatServiceImpl
    @Resource
    lateinit var chatSessionImpl: ChatSessionImpl
    @Resource
    lateinit var chatWebSocketHandler: com.cainsgl.ai.websocket.ChatWebSocketHandler
    /**
     * 获取当前用户的会话列表（分页）
     * @param lastId 上次获取的最后一条记录的ID，首次请求传null
     */
    @GetMapping("/sessions")
    fun getSessions(@RequestParam(required = false) lastId: Long?): List<ChatSessionDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        return if (lastId == null) {
            // 首次请求：优先返回未读会话，为空则返回普通会话列表
            chatService.getUnreadSessions(userId).ifEmpty {
                chatService.getUserSessions(userId, lastId)
            }
        } else {
            // 后续分页请求
            chatService.getUserSessions(userId, lastId)
        }
    }



    @GetMapping("/sessions/byId")
    fun getSessions(@RequestParam(required = true) id: Long): Any {

        val entity= chatSessionImpl.getById(id)?:return ResultCode.RESOURCE_NOT_FOUND
        val userId = StpUtil.getLoginIdAsLong()
        if(entity.userId1!=userId&&entity.userId2!=userId){
            return ResultCode.PERMISSION_DENIED
        }
        return entity
    }
    @GetMapping("/sessions/byOther")
    fun getByOther(@RequestParam(required = true) otherId: Long): Any {
        val currentUserId = StpUtil.getLoginIdAsLong()
        val userId1 = minOf(currentUserId, otherId)
        val userId2 = maxOf(currentUserId, otherId)
        
        val session = chatSessionImpl.baseMapper.findSessionByUsers(userId1, userId2)
            ?: return ResultCode.RESOURCE_NOT_FOUND
        
        // 判断当前用户是 userId1 还是 userId2，返回对应的 deleted 状态
        val isDeleted = if (currentUserId == session.userId1) {
            session.deletedByUser1
        } else {
            session.deletedByUser2
        }
        
        return mapOf(
            "session" to session,
            "deleted" to isDeleted
        )
    }
    /**
     * 获取指定会话的消息列表（游标分页）
     * @param request 上次查询最后一条消息的时间戳，首次查询传null
     * @return 返回最多20条消息和last时间戳
     */
    @PostMapping("/messages")
    fun getMessages(@RequestBody request: GetMessagesRequest): com.cainsgl.ai.dto.response.ChatMessagesResponse {
        val userId = StpUtil.getLoginIdAsLong()
        val response = chatService.getMessages(request.sessionId, userId, request.last)
        return response
    }

    /**
     * 创建或获取与指定用户的会话
     */
    @PostMapping("/session")
    fun getOrCreateSession(@RequestParam otherUserId: Long): ChatSessionEntity {
        val userId = StpUtil.getLoginIdAsLong()
        val session = chatService.getOrCreateSession(userId, otherUserId)
        return session
    }

    /**
     * 删除会话（标记为已删除）
     */
    @DeleteMapping("/session")
    fun deleteSession(@RequestParam sessionId: Long): Any {
        val userId = StpUtil.getLoginIdAsLong()
        
        // 验证会话是否存在以及用户是否有权限
        val session = chatSessionImpl.getById(sessionId) ?: return ResultCode.RESOURCE_NOT_FOUND
        if (session.userId1 != userId && session.userId2 != userId) {
            return ResultCode.PERMISSION_DENIED
        }
        
        // 标记为已删除
        val result = chatSessionImpl.baseMapper.deleteSessionByUser(sessionId, userId)
        return if (result > 0) {
            ResultCode.SUCCESS
        } else {
            ResultCode.SYSTEM_ERROR
        }
    }

    /**
     * 恢复会话（取消删除标记）
     */
    @PutMapping("/session/restore")
    fun restoreSession(@RequestParam sessionId: Long): Any {
        val userId = StpUtil.getLoginIdAsLong()
        
        // 验证会话是否存在以及用户是否有权限
        val session = chatSessionImpl.getById(sessionId) ?: return ResultCode.RESOURCE_NOT_FOUND
        if (session.userId1 != userId && session.userId2 != userId) {
            return ResultCode.PERMISSION_DENIED
        }
        
        // 取消删除标记
        val result = chatSessionImpl.baseMapper.restoreSessionByUser(sessionId, userId)
        return if (result > 0) {
            ResultCode.SUCCESS
        } else {
            ResultCode.SYSTEM_ERROR
        }
    }

    /**
     * 查询已删除的会话列表（分页）
     */
    @GetMapping("/sessions/deleted")
    fun getDeletedSessions(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): Map<String, Any> {
        val userId = StpUtil.getLoginIdAsLong()
        val offset = (page - 1) * pageSize
        
        val sessions = chatSessionImpl.baseMapper.findDeletedSessions(userId, offset, pageSize)
        val total = chatSessionImpl.baseMapper.countDeletedSessions(userId)
        
        return mapOf(
            "list" to sessions,
            "total" to total,
            "page" to page,
            "pageSize" to pageSize
        )
    }

    /**
     * 查询指定用户是否在线
     * 
     * 接口说明：
     * - 通过 HTTP GET 方式查询指定用户的在线状态
     * - 在线状态基于 WebSocket 连接判断
     * 
     * @param userId 要查询的用户ID
     * @return Map 包含以下字段：
     *   - userId: Long - 查询的用户ID
     *   - isOnline: Boolean - 是否在线（true=在线，false=离线）
     * 
     * 使用场景：
     * - 页面初始化时查询对方是否在线
     * - 发送消息前检查对方在线状态
     * - 会话列表显示用户在线状态
     * 
     * 示例请求：
     * GET /chat/user/online?userId=123456
     * 
     * 示例响应：
     * {
     *   "userId": 123456,
     *   "isOnline": true
     * }
     */
    @GetMapping("/user/online")
    fun checkUserOnline(@RequestParam userId: Long): Map<String, Any> {
        val isOnline = chatWebSocketHandler.isUserOnline(userId)
        return mapOf(
            "userId" to userId,
            "isOnline" to isOnline
        )
    }

    /**
     * 批量查询多个用户是否在线
     * 
     * 接口说明：
     * - 通过 HTTP POST 方式批量查询多个用户的在线状态
     * - 适合会话列表、联系人列表等需要批量查询的场景
     * - 建议单次查询不超过 100 个用户
     * 
     * @param userIds 要查询的用户ID列表
     * @return Map 包含以下字段：
     *   - onlineStatus: Map<Long, Boolean> - 用户ID到在线状态的映射
     * 
     * 使用场景：
     * - 会话列表批量显示在线状态
     * - 联系人列表批量查询
     * - 群聊成员在线状态查询
     * 
     * 示例请求：
     * POST /chat/users/online
     * Content-Type: application/json
     * [123456, 789012, 345678]
     * 
     * 示例响应：
     * {
     *   "onlineStatus": {
     *     "123456": true,
     *     "789012": false,
     *     "345678": true
     *   }
     * }
     */
    @PostMapping("/users/online")
    fun checkUsersOnline(@RequestBody userIds: List<Long>): Map<String, Any> {
        val onlineStatus = userIds.associateWith { userId ->
            chatWebSocketHandler.isUserOnline(userId)
        }
        return mapOf(
            "onlineStatus" to onlineStatus
        )
    }
}
