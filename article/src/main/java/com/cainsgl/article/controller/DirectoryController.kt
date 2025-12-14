package com.cainsgl.article.controller

import cn.dev33.satoken.annotation.SaCheckPermission
import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.stp.StpUtil
import com.cainsgl.article.dto.request.CreateDirectoryRequest
import com.cainsgl.article.dto.request.MoveRequest
import com.cainsgl.article.dto.request.ReSortRequest
import com.cainsgl.article.dto.request.UpdateDirectoryRequest
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import jakarta.annotation.Resource
import jakarta.validation.Valid
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/dir")
class DirectoryController
{

    @Resource
    lateinit var directoryService: DirectoryServiceImpl
    @Resource
    lateinit var transactionTemplate: TransactionTemplate
    @SaCheckRole("user")
    @PutMapping
    fun updateDirectory(@RequestBody @Valid request: UpdateDirectoryRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        if (directoryService.updateDirectory(request.id, request.kbId, userId, request.name, request.parentId))
        {
            return ResultCode.SUCCESS
        }
        //尝试推断是什么错误
        if (request.name != null && request.name.isEmpty())
        {
            return ResultCode.PARAM_INVALID
        }
        return ResultCode.RESOURCE_NOT_FOUND
    }

    @SaCheckRole("user")
    @PostMapping("/resort")
    fun resort(@RequestBody @Valid request: ReSortRequest): Any
    {
        // lastId允许为null（表示移到最前面）
        if (request.lastId != null && request.lastId < 0)
        {
            return ResultCode.PARAM_INVALID
        }
        val userId = StpUtil.getLoginIdAsLong()
        val resortDirectory = directoryService.resortDirectory(request.id, request.kbId, userId, request.lastId)
        return resortDirectory
    }

    @SaCheckRole("user")
    @PostMapping("/move")
    fun resortAndUpdate(@RequestBody @Valid request: MoveRequest): Any
    {
        // lastId允许为null（表示移到最前面）
        if (request.lastId != null && request.lastId < 0)
        {
            return ResultCode.PARAM_INVALID
        }
        val userId = StpUtil.getLoginIdAsLong()
        //先去更新目录到对应的父级目录下
       val res= transactionTemplate.execute{ status->
            //这里是有可能抛出运行时异常的
            if (!directoryService.updateDirectory(request.id, request.kbId, userId, null, request.parentId))
            {
                //没成功
                status.setRollbackOnly()
                return@execute ResultCode.DB_ERROR
            }
            val code = directoryService.resortDirectory(request.id, request.kbId, userId, request.lastId)
            if(code!=ResultCode.SUCCESS)
            {
                status.setRollbackOnly()
                return@execute code
            }
            return@execute code
        }

        if (res == null)
        {
            return ResultCode.UNKNOWN_ERROR
        }
        return res
    }

    @SaCheckPermission("directory.post")
    @PostMapping
    fun createDirectory(@RequestBody @Valid request: CreateDirectoryRequest): Any
    {
        //创建新目录，先检查用户是否拥有该kb
        val userId = StpUtil.getLoginIdAsLong()
        if (directoryService.saveDirectory(request.kbId, userId, request.name, request.parentId))
        {
            return ResultCode.SUCCESS
        }
        return ResultCode.DB_ERROR
    }

}
