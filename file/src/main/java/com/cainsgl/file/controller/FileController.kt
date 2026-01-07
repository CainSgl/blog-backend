package com.cainsgl.file.controller


import cn.dev33.satoken.annotation.SaCheckRole
import cn.dev33.satoken.annotation.SaIgnore
import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.cainsgl.api.user.UserService
import com.cainsgl.common.dto.request.CursorList
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.file.FileUrlEntity
import com.cainsgl.common.exception.BusinessException
import com.cainsgl.file.FileService
import com.cainsgl.file.service.FileUrlServiceImpl
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/file")
class FileController
{
    @Resource
    private lateinit var transactionTemplate: TransactionTemplate

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
        if (userService.mallocMemory(userId, file.size.toInt()))
        {
            val fileUrl = fileService.upload(file)
            val fileUrlEntity = FileUrlEntity(
                userId = userId,
                url = fileUrl,
                name = file.originalFilename,
                fileSize = file.size.toInt()
            )
            fileUrlService.save(fileUrlEntity)
            return fileUrlEntity
        } else
        {
            throw BusinessException("你的云存储空间已满，请删除无用文件后再使用！")
        }
    }

    @PostMapping("/list")
    fun cursor(@RequestBody @Valid request: CursorList): Any
    {
        val query = QueryWrapper<FileUrlEntity>().apply {
            eq("user_id", request.id)
            gt("short_url", request.lastId)
            orderByAsc("short_url")
            last("limit 30")
        }
        return fileUrlService.list(query)
    }

    @SaIgnore
    @GetMapping
    fun getFile(
        @RequestParam("f") shorUrl: Long,
        @RequestParam("width", required = false) width: String?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Any?
    {
        if (width != null)
        {
            response.setHeader("X-Image-Render-Width", width)
        }
        val byId = fileUrlService.getById(shorUrl) ?: return null

        response.setHeader("ETag", byId.url);
        //获取请求头里的etag
        val eTag = request.getHeader("If-None-Match")
        if (!eTag.isNullOrEmpty() && eTag == byId.url)
        {
            //内容相同，不需要写入，直接返回
            response.status = HttpServletResponse.SC_NOT_MODIFIED
            return null;
        }
        if (StpUtil.isLogin())
        {
            val roleList = StpUtil.getRoleList()
            if (roleList.contains("admin") || roleList.contains("vip"))
            {
                fileService.getFile(byId.url!!, false, response, byId.name!!)
            } else
            {
                fileService.getFileRateLimit(byId.url!!, false, response, 5, byId.name!!)
            }
            return null
        } else
        {
            fileService.getFileRateLimit(byId.url!!, false, response, 5, byId.name!!)
            return null
        }
    }

    @SaCheckRole("user")
    @GetMapping("/download")
    fun downLoadFile(@RequestParam("f") shorUrl: Long, response: HttpServletResponse): Any?
    {
        val byId = fileUrlService.getById(shorUrl)
        if (byId == null)
        {
            //文件被移除和删除
            return ResultCode.RESOURCE_NOT_FOUND
        } else
        {
            val roleList = StpUtil.getRoleList()
            if (roleList.contains("admin") || roleList.contains("vip"))
            {
                fileService.getFile(byId.url!!, true, response, byId.name!!)
            } else
            {
                fileService.getFileRateLimit(byId.url!!, true, response, 1, byId.name!!)
            }
            return null
        }
    }

    @SaCheckRole("user")
    @GetMapping("/free")
    fun free(@RequestParam("f") shorUrl: Long?): Any
    {
        if (shorUrl == null)
        {
            return ResultCode.NO_DATA
        }
        val query = QueryWrapper<FileUrlEntity>()
        val userId = StpUtil.getLoginIdAsLong()
        query.eq("short_url", shorUrl)
        query.eq("user_id", userId)
        val byId = fileUrlService.getById(shorUrl)
            ?: //文件被移除和删除
            throw BusinessException("释放的文件不存在，可能是已经被删除了，请忽略此消息")

        //删除他
        transactionTemplate.execute {
            val query2 = QueryWrapper<FileUrlEntity>().eq("url", byId.url)
            val count = fileUrlService.count(query2)
            fileUrlService.removeById(byId)
            userService.mallocMemory(userId, -byId.fileSize!!)
            if (count <= 1)
            {
                //需要删除
                fileService.delete(byId.url!!)
            }
        }
        return ResultCode.SUCCESS

    }

    @SaCheckRole("user")
    @GetMapping("/batchFree")
    fun batchFree(@RequestParam("f") shorUrls: List<Long>): Any
    {
        if (shorUrls.isEmpty())
        {
            return ResultCode.NO_DATA
        }
        val userId = StpUtil.getLoginIdAsLong()
        val query = QueryWrapper<FileUrlEntity>().eq("user_id", userId)
        query.`in`("short_url", shorUrls)
        val files = fileUrlService.list(query)
        if (files.isNullOrEmpty())
        {
            return ResultCode.RESOURCE_NOT_FOUND
        }
        val urlToFilesMap: Map<String?, List<FileUrlEntity>> = files.groupBy { it.url }
        var allSize: Int = 0;
        files.forEach { file -> allSize += file.fileSize!! }

        transactionTemplate.execute {
            //看哪些需要删除文件的
            urlToFilesMap.forEach { (fileUrl, groupFiles) ->
                if (fileUrl.isNullOrBlank())
                {
                    return@forEach
                }
                val currentDeleteCount = groupFiles.size
                val totalBeforeDelete = fileUrlService.count(QueryWrapper<FileUrlEntity>().eq("url", fileUrl))
                groupFiles.forEach { file ->
                    fileUrlService.removeById(file)
                }
                if (totalBeforeDelete <= currentDeleteCount)
                {
                    fileService.delete(fileUrl)
                }
            }
            userService.mallocMemory(userId, -allSize)
        }
        return ResultCode.SUCCESS
    }


    @SaCheckRole("user")
    @GetMapping("/list")
    fun list(): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val query = QueryWrapper<FileUrlEntity>().select(FileUrlEntity.BASIC_COL)
        query.eq("user_id", userId)
        return fileUrlService.list(query)
    }

}