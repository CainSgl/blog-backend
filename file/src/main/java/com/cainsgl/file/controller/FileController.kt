package com.cainsgl.file.controller


import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.cainsgl.api.user.UserService
import com.cainsgl.common.annotation.RateLimitByToken
import com.cainsgl.common.dto.request.CursorList
import com.cainsgl.common.dto.response.Result
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.file.FileUrlEntity
import com.cainsgl.common.exception.BusinessException
import com.cainsgl.file.FileService
import com.cainsgl.file.dto.request.PresignedUploadRequest
import com.cainsgl.file.dto.response.PresignedUploadResponse
import com.cainsgl.file.service.FileUrlServiceImpl
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

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
    
    @Resource
    lateinit var fileVerificationService: com.cainsgl.file.service.FileVerificationService

    @RateLimitByToken(message = "文件上传过于频繁", interval = 1000, limit = 1)
    @PostMapping("/presigned-upload")
    fun getPresignedUpload(@RequestBody @Valid request: PresignedUploadRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        
        // 检查用户存储空间
        if (!userService.mallocMemory(userId, request.fileSize.toInt()))
        {
            return com.cainsgl.common.dto.response.Result.error("你的云存储空间已满，请删除无用文件后再使用！")
        }
        
        // 提取文件扩展名
        val extension = request.filename.substringAfterLast(".", "")
        
        // 检查文件是否已存在（通过SHA256去重）
        val existingFile = fileUrlService.baseMapper.findByUserIdAndUrl(request.sha256)
        
        if (existingFile != null)
        {
            // 数据库中有记录，但需要验证 OSS 中文件是否真实存在
            val fileExistsInOss = fileService.isFileExistInOss(request.sha256, extension)
            
            if (fileExistsInOss)
            {
                // 文件在 OSS 中真实存在，无需上传
                return PresignedUploadResponse(
                    url = "",
                    key = "",
                    policy = "",
                    algorithm = "",
                    credential = "",
                    date = "",
                    signature = "",
                    fileId = existingFile.shortUrl,
                    needUpload = false
                )
            } else
            {
                // 数据库有记录但 OSS 文件不存在，删除数据库记录并继续上传流程
                fileUrlService.removeById(existingFile.shortUrl)
            }
        }
        
        // 生成预签名url
        val presignedData = fileService.generatePresignedPostSignature(
            sha256Hash = request.sha256,
            extension = extension
        )
        
        // 创建文件记录
        val fileUrlEntity = FileUrlEntity(
            userId = userId,
            url = request.sha256,
            name = request.filename,
            fileSize = request.fileSize.toInt(),
            status = 0
        )
        fileUrlService.save(fileUrlEntity)
        
        // 添加延迟验证任务（30分钟后验证）
        fileVerificationService.addVerificationTask(fileUrlEntity.shortUrl!!)
        
        return PresignedUploadResponse(
            url = presignedData["url"]!!,
            key = presignedData["key"]!!,
            policy = presignedData["policy"]!!,
            algorithm = presignedData["algorithm"]!!,
            credential = presignedData["credential"]!!,
            date = presignedData["date"]!!,
            signature = presignedData["signature"]!!,
            fileId = fileUrlEntity.shortUrl,
            needUpload = true
        )
    }
   
//    @PostMapping("/upload")
//    fun uploadFile(@RequestParam("file") file: MultipartFile): Any
//    {
//        val userId = StpUtil.getLoginIdAsLong()
//        //增加用户文件大小使用量
//        if (userService.mallocMemory(userId, file.size.toInt()))
//        {
//            val sha256Hash = fileService.upload(file)  // 只返回 SHA256
//            val fileUrlEntity = FileUrlEntity(
//                userId = userId,
//                url = sha256Hash,  // 直接存储 SHA256
//                name = file.originalFilename,
//                fileSize = file.size.toInt()
//            )
//            fileUrlService.save(fileUrlEntity)
//            return fileUrlEntity
//        } else
//        {
//            return com.cainsgl.common.dto.response.Result.error("你的云存储空间已满，请删除无用文件后再使用！")
//        }
//    }

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

        // 重定向到预签名URL（带缓存，7天有效期）
        val downloadUrl = fileService.getDownloadUrl(
            sha256Hash = sha256Hash, 
            extension = extension, 
            expiresInSeconds = 604800,  // 7天
            shortUrl = shortUrl
        )
        response.sendRedirect(downloadUrl)
        return ResultCode.FORWARD
    }


    @GetMapping("/download")
    fun downloadFile(@RequestParam("f") shortUrl: Long, response: HttpServletResponse):Any
    {
        if(shortUrl<100L)
        {
            //忽略
            return ResultCode.SUCCESS
        }
        val fileEntity = fileUrlService.getById(shortUrl) ?: return ResultCode.RESOURCE_NOT_FOUND
        val sha256Hash = fileEntity.url ?: return ResultCode.RESOURCE_NOT_FOUND
        val extension = fileEntity.name?.substringAfterLast(".", "") ?: ""


        if(fileEntity.fileSize!!>5*1024*1024)
        {
            if (!StpUtil.isLogin())
            {
                return Result.error("该文件过大，请先登录后再使用！")
            }
        }

        // 使用缓存的预签名URL，7天有效期
        val downloadUrl = fileService.getDownloadUrl(
            sha256Hash = sha256Hash,
            extension = extension,
            expiresInSeconds = 604800,  // 7天
            isDownload = true,
            filename = fileEntity.name,
            shortUrl = shortUrl
        )
        response.sendRedirect(downloadUrl)
        return ResultCode.FORWARD
    }

    /**
     * 删除单个文件
     */
    @GetMapping("free")
    fun deleteFile(@RequestParam("f", required = false) shortUrl: Long?): Any
    {
        if(shortUrl==null||shortUrl<100L)
        {
            return ResultCode.SUCCESS
        }

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