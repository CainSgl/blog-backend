package com.cainsgl.file.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.file.FileUrlEntity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface FileUrlMapper : BaseMapper<FileUrlEntity> {
    /**
     * 统计指定 SHA256 hash 的引用次数
     * 使用 ByteaTypeHandler 处理 bytea 类型
     */
    fun countByUrl(@Param("url") url: String): Long
}