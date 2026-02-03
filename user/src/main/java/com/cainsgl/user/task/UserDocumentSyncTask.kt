package com.cainsgl.user.task

import com.cainsgl.common.entity.user.UserEntity
import com.cainsgl.user.document.UserDocument
import com.cainsgl.user.service.UserDocumentService
import com.cainsgl.user.service.UserServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * 用户文档同步任务
 * 用于将用户数据同步到 Elasticsearch
 */
@Component
class UserDocumentSyncTask
{
    @Resource
    lateinit var userService: UserServiceImpl

    @Resource
    lateinit var userDocumentService: UserDocumentService

    /**
     * 同步单个用户到 ES
     */
    fun syncUser(userId: Long)
    {
        try
        {
            val user = userService.getById(userId)
            if (user != null && user.nickname != null)
            {
                val userDocument = UserDocument(
                    id = user.id!!,
                    nickname = user.nickname!!
                )
                userDocumentService.save(userDocument)
                log.info { "同步用户到 ES 成功: userId=$userId" }
            }
        } catch (e: Exception)
        {
            log.error(e) { "同步用户到 ES 失败: userId=$userId" }
        }
    }

    /**
     * 批量同步用户到 ES
     */
    fun syncUsers(userIds: List<Long>)
    {
        try
        {
            val users = userService.listByIds(userIds)
            val userDocuments = users.filter { it.nickname != null }.map { user ->
                UserDocument(
                    id = user.id!!,
                    nickname = user.nickname!!
                )
            }
            if (userDocuments.isNotEmpty())
            {
                userDocumentService.saveAll(userDocuments)
                log.info { "批量同步用户到 ES 成功: count=${userDocuments.size}" }
            }
        } catch (e: Exception)
        {
            log.error(e) { "批量同步用户到 ES 失败: userIds=$userIds" }
        }
    }

    /**
     * 全量同步所有用户到 ES
     */
    fun syncAllUsers()
    {
        try
        {
            log.info { "开始全量同步用户到 ES" }
            var page = 1
            val pageSize = 1000
            var hasMore = true

            while (hasMore)
            {
                val users = userService.page(
                    com.baomidou.mybatisplus.extension.plugins.pagination.Page(page.toLong(), pageSize.toLong())
                )
                
                val userDocuments = users.records.filter { it.nickname != null }.map { user ->
                    UserDocument(
                        id = user.id!!,
                        nickname = user.nickname!!
                    )
                }

                if (userDocuments.isNotEmpty())
                {
                    userDocumentService.saveAll(userDocuments)
                    log.info { "同步第 $page 页用户到 ES 成功: count=${userDocuments.size}" }
                }

                hasMore = users.hasNext()
                page++
            }

            log.info { "全量同步用户到 ES 完成" }
        } catch (e: Exception)
        {
            log.error(e) { "全量同步用户到 ES 失败" }
        }
    }

    /**
     * 删除 ES 中的用户文档
     */
    fun deleteUser(userId: Long)
    {
        try
        {
            userDocumentService.delete(userId)
            log.info { "删除 ES 中的用户文档成功: userId=$userId" }
        } catch (e: Exception)
        {
            log.error(e) { "删除 ES 中的用户文档失败: userId=$userId" }
        }
    }
}
