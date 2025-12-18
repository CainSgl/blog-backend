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
        @Param("parentId") parentId: Long?
    ): Int
    /**
     * 插入目录（带权限和数据验证）
     * 验证知识库所有权、父目录kb_id一致性，并自动计算sort_num
     * @param id 目录ID
     * @param kbId 知识库ID
     * @param userId 用户ID
     * @param parentId 父目录ID（可为null表示根目录）
     * @param name 目录名称
     * @param postId 关联文章ID（可为null）
     * @return 插入的行数（1表示成功，0表示验证失败）
     */
    fun insertDirectoryWithValidation(
        @Param("id") id: Long,
        @Param("kbId") kbId: Long,
        @Param("userId") userId: Long,
        @Param("parentId") parentId: Long?,
        @Param("name") name: String,
        @Param("postId") postId: Long?
    ): Int

    /**
     * 获取目录（带权限验证）
     * 验证知识库所有权，并获取指定目录
     * @param id 目录ID
     * @param kbId 知识库ID
     * @param userId 用户ID
     * @return 目录实体（验证失败返回null）
     */
    fun selectDirectoryWithPermissionCheck(
        @Param("id") id: Long,
        @Param("kbId") kbId: Long,
        @Param("userId") userId: Long
    ): DirectoryEntity?

    /**
     * 获取同级目录列表
     * 查询指定知识库下，指定父目录的所有子目录
     * @param kbId 知识库ID
     * @param parentId 父目录ID（null表示根目录）
     * @return 同级目录列表（按sortNum排序）
     */
    fun selectSiblingDirectories(
        @Param("kbId") kbId: Long,
        @Param("parentId") parentId: Long?
    ): List<DirectoryEntity>

    /**
     * 获取同级目录列表
     * 查询指定知识库下，指定父目录的所有子目录
     * @param kbId 知识库ID
     * @param parentId 父目录ID（null表示根目录）
     * @return 同级目录列表（按sortNum排序）
     */
    fun deleteDirectoryWithPermissionCheck(
        @Param("id") id: Long,
        @Param("kbId") kbId: Long,
        @Param("userId") userId: Long
    ): Int
}
