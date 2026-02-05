package com.cainsgl.user.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.cainsgl.common.dto.response.Result
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.user.dto.request.UpdateUserRequest
import com.cainsgl.user.dto.response.UserCurrentResponse
import com.cainsgl.user.dto.response.UserGetResponse
import com.cainsgl.user.service.UserExtraInfoServiceImpl
import com.cainsgl.user.service.UserServiceImpl
import com.cainsgl.user.service.CheckInServiceImpl
import com.cainsgl.user.service.UserDocumentService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*


private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/user")
class UserController
{


    @Resource
    lateinit var userExtraInfoService: UserExtraInfoServiceImpl

    @Resource
    lateinit var userService: UserServiceImpl

    @Resource
    lateinit var checkInService: CheckInServiceImpl

    @Resource
    lateinit var userDocumentService: UserDocumentService


    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    fun getCurrentUser(): UserCurrentResponse
    {
        val userId = StpUtil.getLoginIdAsLong()
        val userInfo = userService.getById(userId)
        //获取自己的热信息
        val hotInfo = userExtraInfoService.getBySaveOnNull(userId)
        return UserCurrentResponse(userInfo!!.calculateLevelInfo().sanitizeSystemSensitiveData(), hotInfo)
    }


    @GetMapping
    fun get(@RequestParam id: Long): Any
    {
        val user = userService.getById(id) ?: return ResultCode.RESOURCE_NOT_FOUND
        //去除敏感字段
        user.calculateLevelInfo()
        val hotInfo = userExtraInfoService.getBySaveOnNull(user.id!!)
        return UserGetResponse(user, hotInfo)
    }

    @PutMapping
    fun update(@RequestBody request: UpdateUserRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        
        if (request.username != null)
        {
            val update = KtUpdateWrapper(UserEntity::class.java).eq(UserEntity::id, userId).isNull(UserEntity::username)
                .ne(UserEntity::username, request.username).set(UserEntity::username, request.username)
            return if (userService.update(update))
            {
                ResultCode.SUCCESS
            } else
            {
                Result.error("用户名重复，请重新输入")
            }
        }
        
        val userEntity = UserEntity(id = userId).apply {
            nickname = request.nickname
            avatarUrl = request.avatarUrl
            bio = request.bio
            gender = request.gender
        }
        userService.updateById(userEntity)
        
        // 如果昵称有更新，同步到 ES（updateById 中已处理，这里是双重保险）
        if (request.nickname != null)
        {
            try
            {
                userDocumentService.save(com.cainsgl.user.document.UserDocument(id = userId, nickname = request.nickname))
            } catch (e: Exception)
            {
                log.warn { "同步用户昵称到 ES 失败: userId=$userId, error=${e.message}" }
            }
        }
        
        return ResultCode.SUCCESS
    }

    @GetMapping("/search")
    fun search(
        @RequestParam keyword: String,
        @RequestParam(required = false) size: Int = 20,
        @RequestParam(required = false) searchAfter: List<Any>? = null
    ): Any
    {
        // 先尝试根据 ID 精确搜索
        val userId = keyword.toLongOrNull()
        if (userId != null)
        {
            val user = userService.getById(userId)
            if (user != null)
            {
                return mapOf(
                    "data" to listOf(UserGetResponse(user, userExtraInfoService.getBySaveOnNull(user.id!!))),
                    "total" to 1L,
                    "hasMore" to false,
                    "searchAfter" to null
                )
            }
        }

        // 使用 ES 搜索昵称
        val searchResult = userDocumentService.search(keyword, size, searchAfter)
        
        // 根据 ES 返回的 ID 列表，从数据库回表查询完整用户信息
        val userIds = searchResult.data.map { it.id }
        if (userIds.isEmpty())
        {
            return mapOf(
                "data" to emptyList<UserGetResponse>(),
                "total" to 0L,
                "hasMore" to false,
                "searchAfter" to null
            )
        }

        // 批量查询用户信息
        val users = userService.listByIds(userIds)
        val userMap = users.associateBy { it.id!! }

        // 按照 ES 返回的顺序组装结果，并附加热信息
        val result = searchResult.data.mapNotNull { doc ->
            userMap[doc.id]?.let { user ->
                UserGetResponse(
                    user.calculateLevelInfo().sanitizeSystemSensitiveData(),
                    userExtraInfoService.getBySaveOnNull(user.id!!)
                )
            }
        }

        return mapOf(
            "data" to result,
            "total" to searchResult.total,
            "hasMore" to searchResult.hasMore,
            "searchAfter" to searchResult.searchAfter
        )
    }

    /**
     * 用户签到
     */
    @PostMapping("/checkin")
    fun checkIn(): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        return checkInService.checkIn(userId)
    }
}
