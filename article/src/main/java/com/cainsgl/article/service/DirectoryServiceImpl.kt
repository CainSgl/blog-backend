package com.cainsgl.article.service

import com.baomidou.mybatisplus.core.toolkit.IdWorker
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.article.dto.DirectoryTreeDTO
import com.cainsgl.article.repository.DirectoryMapper
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.DirectoryEntity
import com.cainsgl.common.service.article.directory.DirectoryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class DirectoryServiceImpl : ServiceImpl<DirectoryMapper, DirectoryEntity>(), DirectoryService, IService<DirectoryEntity>
{


    /**
     * 递归查询知识库的完整目录树结构
     * @param kbId 知识库ID
     * @return 目录树列表(包含关联的Post信息)
     */
    fun getDirectoryTreeByKbId(kbId: Long): List<DirectoryTreeDTO>
    {
        val list = baseMapper.getDirectoryTreeByKbId(kbId)
        if (list.isNullOrEmpty())
        {
            return emptyList()
        }

        // 创建ID到节点的映射
        val nodeMap = list.associateBy { it.id }

        // 递归排序子节点
        fun sortChildren(nodes: List<DirectoryTreeDTO>)
        {
            nodes.forEach { node ->
                node.children?.let {
                    node.children = it.sortedBy { child -> child.sortNum }.also(::sortChildren)
                }
            }
        }

        // 找出所有根节点
        val rootNodes = list.filter { it.parentId == null }
        // 将子节点添加到对应的父节点中
        list.filter { it.parentId != null }.forEach { node ->
            nodeMap[node.parentId]?.let { parent ->
                parent.children = (parent.children ?: mutableListOf()).toMutableList().apply { add(node) }
            }
        }
        // 对所有层级进行排序
        sortChildren(rootNodes)
        return rootNodes
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

    fun saveDirectory(kbId: Long, userId: Long, name: String, parentId: Long? = null, postId: Long? = null): Boolean
    {
        val baseMapper = getBaseMapper()
        try
        {
            return baseMapper.insertDirectoryWithValidation(
                id = IdWorker.getId(), kbId = kbId, userId = userId, parentId = parentId, name = name, postId = postId
            ) > 0
        } catch (e: Exception)
        {
            //可能是参数不对或者null
            log.warn(e.message)
            return false
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
    @Transactional(propagation = Propagation.SUPPORTS)
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
}
