package com.cainsgl.file.controller


import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
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

   
    @PostMapping("/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        //增加用户文件大小使用量
        if (userService.mallocMemory(userId, file.size.toInt()))
        {
            val sha256Hash = fileService.upload(file)  // 只返回 SHA256
            val fileUrlEntity = FileUrlEntity(
                userId = userId,
                url = sha256Hash,  // 直接存储 SHA256
                name = file.originalFilename,
                fileSize = file.size.toInt()
            )
            fileUrlService.save(fileUrlEntity)
            return fileUrlEntity
        } else
        {
            return com.cainsgl.common.dto.response.Result.error("你的云存储空间已满，请删除无用文件后再使用！")
        }
    }

    @PostMapping("/list")
    fun cursor(@RequestBody @Valid request: CursorList): Any
    {
        val query = KtQueryWrapper(FileUrlEntity::class.java).apply {
            eq(FileUrlEntity::userId, request.id)
            gt(FileUrlEntity::shortUrl, request.lastId)
            orderByAsc(FileUrlEntity::shortUrl)
            last("limit 30")
        }
        return fileUrlService.list(query)
    }

    /**
     * 公开访问文件 - 通过重定向到预签名URL
     * 支持ETag缓存优化
     */
    @GetMapping
    fun getFile(
        @RequestParam("f") shortUrl: Long,
        @RequestParam("width", required = false) width: String?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ):Any
    {
        val fileEntity = fileUrlService.getById(shortUrl) 
            ?: return ResultCode.RESOURCE_NOT_FOUND

        val sha256Hash = fileEntity.url ?: return ResultCode.RESOURCE_NOT_FOUND
        val extension = fileEntity.name?.substringAfterLast(".", "") ?: ""

        // ETag缓存检查（使用 hash 作为 ETag）
        val eTag = request.getHeader("If-None-Match")
        if (!eTag.isNullOrEmpty() && eTag == sha256Hash)
        {
            response.status = HttpServletResponse.SC_NOT_MODIFIED
            response.setHeader("ETag", sha256Hash)
            return ResultCode.SUCCESS
        }

        // 设置响应头
        response.setHeader("ETag", sha256Hash)

        // 重定向到预签名URL
        val downloadUrl = fileService.getDownloadUrl(sha256Hash, extension, expiresInSeconds = 300)
        response.sendRedirect(downloadUrl)
        return ResultCode.FORWARD
    }


    @GetMapping("/download")
    fun downloadFile(@RequestParam("f") shortUrl: Long, response: HttpServletResponse):Any
    {
        val fileEntity = fileUrlService.getById(shortUrl)
            ?: return ResultCode.RESOURCE_NOT_FOUND
        
        val sha256Hash = fileEntity.url ?: return ResultCode.RESOURCE_NOT_FOUND
        val extension = fileEntity.name?.substringAfterLast(".", "") ?: ""
            
        val downloadUrl = fileService.getDownloadUrl(
            sha256Hash = sha256Hash,
            extension = extension,
            expiresInSeconds = 30,
            isDownload = true,
            filename = fileEntity.name
        )
        response.sendRedirect(downloadUrl)
        return ResultCode.FORWARD
    }

    /**
     * 删除单个文件
     */
    @GetMapping("free")
    fun deleteFile(@RequestParam("f") shortUrl: Long): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val fileEntity = fileUrlService.getById(shortUrl)
            ?: throw BusinessException("文件不存在或已被删除")

        // 验证文件所有权
        if (fileEntity.userId != userId)
        {
            return ResultCode.PERMISSION_DENIED
        }
        fileUrlService.deleteFileInternal(listOf(fileEntity), userId)
        return ResultCode.SUCCESS
    }

    /**
     * 批量删除文件
     */
    @GetMapping("/batchFree")
    fun batchDeleteFiles(@RequestParam("f") shortUrls: List<Long>): Any
    {
        if (shortUrls.isEmpty())
        {
           return ResultCode.MISSING_PARAM
        }

        val userId = StpUtil.getLoginIdAsLong()
        val files = fileUrlService.list(
            KtQueryWrapper(FileUrlEntity::class.java)
                .eq(FileUrlEntity::userId, userId)
                .`in`(FileUrlEntity::shortUrl, shortUrls)
        )

        if (files.isNullOrEmpty())
        {
          return ResultCode.RESOURCE_NOT_FOUND
        }

        fileUrlService.deleteFileInternal(files, userId)
        return ResultCode.SUCCESS
    }



    /**
     * 获取用户的文件列表
     */
   
    @GetMapping("/list")
    fun listUserFiles(): List<FileUrlEntity>
    {
        val userId = StpUtil.getLoginIdAsLong()
        return fileUrlService.list(
            QueryWrapper<FileUrlEntity>()
                .select(FileUrlEntity.BASIC_COL)
                .eq("user_id", userId)
        )
    }

}