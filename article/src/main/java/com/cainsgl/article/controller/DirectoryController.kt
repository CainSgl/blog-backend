package com.cainsgl.article.controller

import cn.dev33.satoken.annotation.SaCheckPermission
import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.article.dto.request.dir.CreateDirectoryRequest
import com.cainsgl.article.dto.request.dir.UpdateDirectoryRequest
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/dir")
class DirectoryController
{

    @Resource
    lateinit var directoryService: DirectoryServiceImpl


    @SaCheckRole("user")
    @PutMapping
    fun updateDirectory(@RequestBody request: UpdateDirectoryRequest): Any
    {
        requireNotNull(request.id) { return ResultCode.MISSING_PARAM }
        require(request.id >= 0) { return ResultCode.PARAM_INVALID }
        requireNotNull(request.kbId) { return ResultCode.MISSING_PARAM }
        val userId = StpUtil.getLoginIdAsLong()
        if (directoryService.updateDirectory(request.id, request.kbId, userId, request.name, request.parentId, request.sortNum))
        {
            return ResultCode.SUCCESS
        }
        //尝试推断是什么错误
        if (request.name != null && request.name.isEmpty())
        {
            return ResultCode.PARAM_INVALID
        }
        if (request.sortNum != null && request.sortNum < 0)
        {
            return ResultCode.PARAM_INVALID
        }
        return ResultCode.DB_ERROR
    }
    @SaCheckPermission("directory.post")
    @PostMapping
    fun createDirectory(@RequestBody request: CreateDirectoryRequest): Any
    {
        requireNotNull(request.kbId) { return ResultCode.MISSING_PARAM }
        require(request.kbId >= 0) { return ResultCode.PARAM_INVALID }
        //创建新目录，先检查用户是否拥有该kb
        val userId = StpUtil.getLoginIdAsLong()
        if (directoryService.saveDirectory(request.kbId,userId, request.name, request.parentId))
        {
            return ResultCode.SUCCESS
        }
        return ResultCode.DB_ERROR
    }

}
