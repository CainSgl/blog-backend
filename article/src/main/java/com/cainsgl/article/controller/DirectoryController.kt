package com.cainsgl.article.controller

import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.article.dto.request.UpdateDirectoryRequest
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/directory")
class DirectoryController
{

    @Resource
    lateinit var directoryService: DirectoryServiceImpl


    @SaCheckRole("user")
    @PutMapping
    fun updateDirectory(@RequestBody request: UpdateDirectoryRequest): Any
    {
        if (request.id == null || request.id < 0)
        {
            return ResultCode.MISSING_PARAM
        }
        if (request.kbId == null)
        {
            return ResultCode.MISSING_PARAM
        }
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
}
