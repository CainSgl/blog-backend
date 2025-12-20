package com.cainsgl.file.repository

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.cainsgl.common.entity.file.FileUrlEntity
import org.apache.ibatis.annotations.Mapper

@Mapper
interface FileUrlMapper : BaseMapper<FileUrlEntity> {

}