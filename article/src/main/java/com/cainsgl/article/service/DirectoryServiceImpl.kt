package com.cainsgl.article.service

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.core.toolkit.IdWorker
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.article.directory.DirectoryService
import com.cainsgl.article.dto.DirectoryTreeDTO
import com.cainsgl.article.repository.DirectoryMapper
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.DirectoryEntity
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import com.cainsgl.common.util.FineLockCacheUtils.getWithFineLock
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
class DirectoryServiceImpl : ServiceImpl<DirectoryMapper, DirectoryEntity>(), DirectoryService,
    IService<DirectoryEntity>
{
    @Resource
    lateinit var knowledgeBaseService: KnowledgeBaseServiceImpl


    @Resource
    lateinit var redisTemplate: RedisTemplate<String, List<DirectoryTreeDTO>>

    companion object
    {
        const val DIR_REDIS_PRE_FIX = "dir:"
    }
    /**
     * 递归查询知识库的完整目录树结构
     * @param kbId 知识库ID
     * @return 目录树列表(包含关联的Post信息)
     */
    fun getDirectoryTreeByKbId(kbId: Long): List<DirectoryTreeDTO>
    {
        //根据id去缓存
        if (kbId < 0)
        {
            return emptyList();
        }
        return redisTemplate.getWithFineLock("$DIR_REDIS_PRE_FIX$kbId",Duration.ofMinutes(10)
        ,{
            val list = baseMapper.getDirectoryTreeByKbId(kbId)
            if (list.isNullOrEmpty())
            {
                return@getWithFineLock emptyList()
            }

            // 创建ID到节点的映射
            val nodeMap = list.associateBy { it.id }
            // 递归排序节点
            fun sortChildren(nodes: List<DirectoryTreeDTO>)
            {
                nodes.forEach { node ->
                    node.children.let {
                        node.children = it.sortedBy { child -> child.sortNum }.also(::sortChildren)
                    }
                }
            }
            // 找出所有根节点
            val rootNodes = list.filter { it.parentId == null }
            // 将子节点添加到对应的父节点中
            list.filter { it.parentId != null }.forEach { node ->
                nodeMap[node.parentId]?.let { parent ->
                    parent.children = parent.children.toMutableList().apply { add(node) }
                }
            }
            // 对所有层级进行排序
            sortChildren(rootNodes)

            return@getWithFineLock rootNodes
        }
        )?: emptyList()
    }
    fun removeCache(kbId: Long)
    {
        redisTemplate.delete("$DIR_REDIS_PRE_FIX$kbId")
        Thread.ofVirtual().start{
            Thread.sleep(1000)
            redisTemplate.delete("$DIR_REDIS_PRE_FIX$kbId")
        }
    }
    fun getDirectoryWithPermissionCheck(directoryId: Long, kbId: Long, userId: Long): DirectoryEntity?
    {
        return baseMapper.selectDirectoryWithPermissionCheck(directoryId, kbId, userId)
    }

    fun updateDirectory(id: Long, kbId: Long, userId: Long, name: String?, parentId: Long?): Boolean
    {
        val baseMapper = getBaseMapper()
        if (name != null && name.isEmpty())
        {
            //这里是有问题的，因为name不为null，说明要更新，但是这里的name却是""
            return false
        }
        try
        {
            return baseMapper.updateDirectoryWithPermissionCheck(
                id, kbId, userId, name, parentId
            ) > 0
        } catch (e: Exception)
        {
            //可能是参数不对或者null
            log.warn(e.message)
            return false
        }
    }
    @Transactional(propagation = Propagation.REQUIRED)
    fun saveDirectory(kbId: Long, userId: Long, name: String, parentId: Long? = null, postId: Long? = null): Long
    {
        val baseMapper = getBaseMapper()
        try
        {
            val directoryId = IdWorker.getId()
            val success = baseMapper.insertDirectoryWithValidation(
                directoryId, kbId = kbId, userId = userId, parentId = parentId, name = name, postId = postId
            ) > 0
            //去增加kb的post_count
            if(postId!=null)
            {
                val query= UpdateWrapper <KnowledgeBaseEntity>().eq("id",kbId).setSql("post_count = post_count + 1");
                knowledgeBaseService.update(query)
            }
            return if (success)
            {
                directoryId
            } else
            {
                -1
            }
        } catch (e: Exception)
        {
            //可能是参数不对或者null
            log.warn(e.message)
            return -1
        }
    }

    /**
     * 重新排序目录
     * @param id 要移动的目录ID
     * @param kbId 知识库ID
     * @param userId 用户ID
     * @param lastId 目标位置的前一个目录ID（null表示移到最前面）
     * @return 是否成功
     */
    @Transactional(propagation = Propagation.REQUIRED)
    fun resortDirectory(id: Long, kbId: Long, userId: Long, lastId: Long?): ResultCode
    {
        // 获取要移动的目录
        val targetDir =
            baseMapper.selectDirectoryWithPermissionCheck(id, kbId, userId) ?: return ResultCode.RESOURCE_NOT_FOUND

        // 获取所有同级目录（parent_id相同，kb_id相同）
        val siblings = baseMapper.selectSiblingDirectories(kbId, targetDir.parentId).toMutableList()
        if (siblings.isEmpty()) return ResultCode.UNKNOWN_ERROR
        // 验证lastId（如果不为null）
        if (lastId != null)
        {
            //没有上一个目录，或者找不到
            siblings.find { it.id == lastId } ?: return ResultCode.RESOURCE_NOT_FOUND
        }
        // 移除要移动的目录
        siblings.removeIf { it.id == id }
        val insertIndex = if (lastId == null)
        {
            0  // 移到最前面
        } else
        {
            val lastIndex = siblings.indexOfFirst { it.id == lastId }
            if (lastIndex == -1) return ResultCode.UNKNOWN_ERROR
            lastIndex + 1  // 插入到lastId后面
        }
        siblings.add(insertIndex, targetDir)
        // 重新设置sortNum（1, 2, 3...）
        siblings.forEachIndexed { index, dir ->
            dir.sortNum = (index + 1).toShort()
        }
        if (updateBatchById(siblings))
        {
            return ResultCode.SUCCESS
        }
        return ResultCode.DB_ERROR
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Deprecated(
        "废弃，因为可以在获取的时候检验",
        ReplaceWith("baseMapper.deleteDirectoryWithPermissionCheck(id, kbId, userId) > 0")
    )
    fun deleteDirectory(id: Long, kbId: Long, userId: Long): Boolean
    {
        return baseMapper.deleteDirectoryWithPermissionCheck(id, kbId, userId) > 0
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    fun getDirectoryAndSubdirectories(directoryId: Long): List<DirectoryEntity>?
    {
        return baseMapper.getDirectoryAndSubdirectories(directoryId)
    }


}
