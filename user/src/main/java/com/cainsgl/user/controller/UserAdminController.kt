package com.cainsgl.user.controller

import cn.dev33.satoken.annotation.SaCheckRole
import com.cainsgl.common.dto.response.Result
import com.cainsgl.user.task.UserDocumentSyncTask
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/user/admin")
@SaCheckRole("admin")
class UserAdminController
{
    @Resource
    lateinit var userDocumentSyncTask: UserDocumentSyncTask

    /**
     * 全量同步用户到 ES
     */
    @PostMapping("/sync-all-to-es")
    fun syncAllToEs(): Any
    {
        return try
        {
            userDocumentSyncTask.syncAllUsers()
            Result.success("全量同步任务已启动")
        } catch (e: Exception)
        {
            log.error(e) { "全量同步用户到 ES 失败" }
            Result.error("全量同步失败: ${e.message}")
        }
    }

    /**
     * 同步指定用户到 ES
     */
    @PostMapping("/sync-user-to-es")
    fun syncUserToEs(@RequestParam userId: Long): Any
    {
        return try
        {
            userDocumentSyncTask.syncUser(userId)
            Result.success("用户同步成功")
        } catch (e: Exception)
        {
            log.error(e) { "同步用户到 ES 失败: userId=$userId" }
            Result.error("同步失败: ${e.message}")
        }
    }

    /**
     * 批量同步用户到 ES
     */
    @PostMapping("/sync-users-to-es")
    fun syncUsersToEs(@RequestBody userIds: List<Long>): Any
    {
        return try
        {
            userDocumentSyncTask.syncUsers(userIds)
            Result.success("批量同步成功")
        } catch (e: Exception)
        {
            log.error(e) { "批量同步用户到 ES 失败" }
            Result.error("批量同步失败: ${e.message}")
        }
    }

    /**
     * 删除 ES 中的用户文档
     */
    @DeleteMapping("/delete-user-from-es")
    fun deleteUserFromEs(@RequestParam userId: Long): Any
    {
        return try
        {
            userDocumentSyncTask.deleteUser(userId)
            Result.success("删除成功")
        } catch (e: Exception)
        {
            log.error(e) { "删除 ES 中的用户文档失败: userId=$userId" }
            Result.error("删除失败: ${e.message}")
        }
    }
}
