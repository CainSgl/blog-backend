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

    /**
     * 公开访问文件 - 通过重定向到预签名URL
     * 支持ETag缓存优化
     */
    @SaIgnore
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

        // ETag缓存检查
        val eTag = request.getHeader("If-None-Match")
        if (!eTag.isNullOrEmpty() && eTag == fileEntity.url)
        {
            response.status = HttpServletResponse.SC_NOT_MODIFIED
            response.setHeader("ETag", fileEntity.url)
            return ResultCode.SUCCESS
        }

        // 设置响应头
        response.setHeader("ETag", fileEntity.url)
//        if (width != null)
//        {
//            response.setHeader("X-Image-Render-Width", width)
//        }

        // 重定向到预签名URL
        val downloadUrl = fileService.getDownloadUrl(fileEntity.url!!, expiresInSeconds = 300)
        response.sendRedirect(downloadUrl)
        return ResultCode.SUCCESS
    }

   // @SaCheckRole("user")
    @GetMapping("/download")
    fun downloadFile(@RequestParam("f") shortUrl: Long, response: HttpServletResponse):Any
    {
        val fileEntity = fileUrlService.getById(shortUrl)
            ?: return ResultCode.RESOURCE_NOT_FOUND
        val downloadUrl = fileService.getDownloadUrl(
            objectKey = fileEntity.url!!,
            expiresInSeconds = 30,
            isDownload = true,
            filename = fileEntity.name
        )
        response.sendRedirect(downloadUrl)
        return ResultCode.SUCCESS
    }


    /**
     * 删除单个文件
     */
    @SaCheckRole("user")
    @DeleteMapping
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

        deleteFileInternal(listOf(fileEntity), userId)
        return ResultCode.SUCCESS
    }

    /**
     * 批量删除文件
     */
    @SaCheckRole("user")
    @DeleteMapping("/batch")
    fun batchDeleteFiles(@RequestParam("f") shortUrls: List<Long>): Any
    {
        if (shortUrls.isEmpty())
        {
           return ResultCode.MISSING_PARAM
        }

        val userId = StpUtil.getLoginIdAsLong()
        val files = fileUrlService.list(
            QueryWrapper<FileUrlEntity>()
                .eq("user_id", userId)
                .`in`("short_url", shortUrls)
        )

        if (files.isNullOrEmpty())
        {
          return ResultCode.RESOURCE_NOT_FOUND
        }

        deleteFileInternal(files, userId)
        return ResultCode.SUCCESS
    }

    /**
     * 删除文件的内部实现
     * 处理引用计数和实际文件删除
     */
    private fun deleteFileInternal(files: List<FileUrlEntity>, userId: Long)
    {
        val urlToFilesMap = files.groupBy { it.url }
        val totalSize = files.sumOf { it.fileSize ?: 0 }

        transactionTemplate.execute {
            urlToFilesMap.forEach { (fileUrl, groupFiles) ->
                if (fileUrl.isNullOrBlank()) return@forEach

                val currentDeleteCount = groupFiles.size
                val totalReferences = fileUrlService.count(
                    QueryWrapper<FileUrlEntity>().eq("url", fileUrl)
                )

                // 删除数据库记录
                groupFiles.forEach { file -> fileUrlService.removeById(file) }

                // 如果没有其他引用，删除实际文件
                if (totalReferences <= currentDeleteCount)
                {
                    fileService.delete(fileUrl)
                }
            }
            
            // 释放用户存储空间
            userService.mallocMemory(userId, -totalSize)
        }
    }

    /**
     * 获取用户的文件列表
     */
    @SaCheckRole("user")
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