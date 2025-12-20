package com.cainsgl.file.controller

import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.annotation.SaIgnore
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.cainsgl.api.user.UserService
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.file.FileUrlEntity
import com.cainsgl.common.exception.BusinessException
import com.cainsgl.file.FileService
import com.cainsgl.file.service.FileUrlServiceImpl
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/file")
class FileController
{
    @Resource
    lateinit var fileService: FileService
    @Resource
    lateinit var fileUrlService: FileUrlServiceImpl
    @Resource
    lateinit var userService: UserService
    @SaCheckRole("user")
    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): FileUrlEntity
    {
        val userId = StpUtil.getLoginIdAsLong()
        //增加用户文件大小使用量
        if (userService.mallocMemory(userId,file.size.toInt()))
        {
            val fileUrl = fileService.upload(file)
            val fileUrlEntity = FileUrlEntity(userId = userId, url = fileUrl,name=file.originalFilename)
            fileUrlService.save(fileUrlEntity)
            return fileUrlEntity
        }else
        {
            throw BusinessException("你的内存使用空间已达到最大，无法再上传文件！")
        }
    }
    @SaIgnore
    @GetMapping
    fun getFile(@RequestParam("f") shorUrl: Long, response: HttpServletResponse):Any?
    {
        val byId = fileUrlService.getById(shorUrl)
        if(byId==null)
        {
            //文件被移除和删除
            return ResultCode.RESOURCE_NOT_FOUND
        }else
        {
            if(StpUtil.isLogin())
            {
                val roleList = StpUtil.getRoleList()
                if(roleList.contains("admin")||roleList.contains("vip"))
                {
                    fileService.getFile(byId.url!!,false,response,byId.name!!)
                }else
                {
                    fileService.getFileRateLimit(byId.url!!,false,response,5,byId.name!!)
                }
                return null
            }else
            {
                fileService.getFileRateLimit(byId.url!!,false,response,5,byId.name!!)
                return null
            }
        }
    }
    @SaCheckRole("user")
    @GetMapping("download")
    fun downLoadFile(@RequestParam("f") shorUrl: Long, response: HttpServletResponse):Any?
    {
        val byId = fileUrlService.getById(shorUrl)
        if(byId==null)
        {
            //文件被移除和删除
            return ResultCode.RESOURCE_NOT_FOUND
        }else
        {
            val roleList = StpUtil.getRoleList()
            if(roleList.contains("admin")||roleList.contains("vip"))
            {
                fileService.getFile(byId.url!!,true,response,byId.name!!)
            }else
            {
                fileService.getFileRateLimit(byId.url!!,true,response,1,byId.name!!)
            }
            return null
        }
    }

    @SaCheckRole("user")
    @GetMapping("free")
    fun free(@RequestParam("f") shorUrl: Long, response: HttpServletResponse):Any?
    {
        val query=QueryWrapper<FileUrlEntity>()
        val userId = StpUtil.getLoginIdAsLong()
        query.eq("short_url",shorUrl)
        query.eq("user_id",userId)
        val byId = fileUrlService.getById(shorUrl)
        if(byId==null)
        {
            //文件被移除和删除
            return ResultCode.RESOURCE_NOT_FOUND
        }else
        {
           //删除他
            fileService.delete(byId.url!!)
            userService.mallocMemory(userId,)
            fileUrlService.removeById(byId)
            return null
        }
    }
}