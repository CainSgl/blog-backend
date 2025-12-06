package com.cainsgl.article.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.article.dto.DirectoryTreeDTO
import com.cainsgl.common.entity.article.DirectoryEntity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface DirectoryMapper : BaseMapper<DirectoryEntity> {

    /**
     * 递归查询知识库的完整目录树结构
     * @param kbId 知识库ID
     * @return 目录树列表（包含关联的Post信息）
     */
    fun getDirectoryTreeByKbId(@Param("kbId") kbId: Long): List<DirectoryTreeDTO>?

    /**
     * 更新目录信息（带权限校验）
     * 通过JOIN知识库表验证用户权限，确保目录属于用户的知识库
     * @param directoryId 目录ID
     * @param kbId 知识库ID
     * @param userId 用户ID
     * @param name 目录名称
     * @param parentId 父目录ID
     * @param sortNum 排序号
     * @return 更新的行数（1表示成功，0表示无权限或目录不存在）
     */
    fun updateDirectoryWithPermissionCheck(
        @Param("directoryId") directoryId: Long,
        @Param("kbId") kbId: Long,
        @Param("userId") userId: Long,
        @Param("name") name: String?,
        @Param("parentId") parentId: Long?,
        @Param("sortNum") sortNum: Short?
    ): Int

}
