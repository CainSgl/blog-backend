package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.article.dto.DirectoryTreeDTO
import com.cainsgl.article.repository.DirectoryMapper
import com.cainsgl.common.entity.article.DirectoryEntity
import com.cainsgl.common.service.article.directory.DirectoryService
import org.springframework.stereotype.Service

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


    fun updateDirectory(id:Long,kbId: Long,userId: Long,name:String?,parentId: Long?,sortNum:Short?): Boolean
    {
        val baseMapper = getBaseMapper()
        if (name != null&&name.isEmpty())
        {
            //这里是有问题的，因为name不为null，说明要更新，但是这里的name却是""
            return false
        }
        if(sortNum != null&&sortNum<0)
        {
            //排序号不能小于0
            return false
        }
        try
        {
           return baseMapper.updateDirectoryWithPermissionCheck(
                id,
                kbId,
                userId,
                name,
                parentId,
                sortNum
            )>0
        }catch (e: Exception)
        {
            //可能是参数不对或者null
            log.warn(e.message)
            return false
        }

    }

}
