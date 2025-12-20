package com.cainsgl.file.service

import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.common.entity.file.FileUrlEntity
import com.cainsgl.file.repository.FileUrlMapper
import org.springframework.stereotype.Service

@Service
class FileUrlServiceImpl : ServiceImpl<FileUrlMapper, FileUrlEntity>(), IService<FileUrlEntity> {

}