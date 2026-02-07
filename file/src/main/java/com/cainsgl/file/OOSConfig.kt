package com.cainsgl.file

import com.cainsgl.common.exception.BSystemException
import com.volcengine.tos.*
import com.volcengine.tos.auth.StaticCredentials
import com.volcengine.tos.comm.HttpMethod
import com.volcengine.tos.model.`object`.*
import com.volcengine.tos.transport.TransportConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException


@Configuration
class OOSConfig(@Value("\${oos.secretId}") var accessKey: String, @Value("\${oos.secretKey}") var secretKey: String)
{
    @Bean
    fun oosClient(): TOSV2
    {
        val endpoint = "tos-cn-beijing.volces.com"
        val region = "cn-beijing"
        val connectTimeoutMills = 20000
        val config = TransportConfig.builder().connectTimeoutMills(connectTimeoutMills).build()

        val configuration = TOSClientConfiguration.builder().transportConfig(config).region(region).endpoint(endpoint)
            .credentials(StaticCredentials(accessKey, secretKey)).build()
        val tos = TOSV2ClientBuilder().build(configuration)

        return tos
    }
    @Bean
    fun frontOssClient(): TOSV2
    {
        val endpoint = "tos-cn-hongkong.volces.com"
        val region = "hongkong"
        val connectTimeoutMills = 20000
        val config = TransportConfig.builder().connectTimeoutMills(connectTimeoutMills).build()

        val configuration = TOSClientConfiguration.builder().transportConfig(config).region(region).endpoint(endpoint)
            .credentials(StaticCredentials(accessKey, secretKey)).build()
        val tos = TOSV2ClientBuilder().build(configuration)

        return tos
    }
}

private val log = KotlinLogging.logger {}

@Component
class FileService
{
    @Resource(name = "oosClient")
    lateinit var tos: TOSV2

    @Value("\${oos.bucketName}")
    lateinit var bucketName: String

    @Resource
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Throws(IOException::class)
    fun upload(file: MultipartFile): String
    {
        if (file.isEmpty)
        {
            throw BSystemException("上传的文件不能为空")
        }
        val originalFilename = file.originalFilename!!
        val fileSha256: String = calculateFileSHA256(file)  // 只返回 SHA256
        var fileSuffix = ""
        if (originalFilename.contains("."))
        {
            fileSuffix = originalFilename.substring(originalFilename.lastIndexOf("."))
        }
        val objectKey: String = bucketName + "file/" + fileSha256 + fileSuffix
        if (isFileExistInCos(objectKey))
        {
            // 已存在，直接返回 SHA256，无需重复上传
            return fileSha256
        }
        file.inputStream.use { stream ->
            val putObjectInput =
                PutObjectInput().setBucket(bucketName).setKey(objectKey).setContent(stream).setContentLength(file.size)
            val output: PutObjectOutput = tos.putObject(putObjectInput)
            log.info { "putObject succeed, object's etag is " + output.etag }
            log.info { "putObject succeed, object's crc64 is " + output.hashCrc64ecma }
            return fileSha256  // 只返回 SHA256
        }
    }

    /**
     * 从 SHA256 hash 构建完整的对象存储 key
     * @param sha256Hash SHA256 hash 字符串
     * @param extension 文件扩展名（可选）
     * @return 完整的对象存储 key，格式: bucketName/file/{sha256}.{ext}
     */
    fun buildObjectKey(sha256Hash: String, extension: String? = null): String
    {
        val ext = if (!extension.isNullOrEmpty())
        {
            if (extension.startsWith(".")) extension else ".$extension"
        } else ""
        return "${bucketName}file/$sha256Hash$ext"
    }

    /**
     * 生成预签名下载URL（带Redis缓存）
     * @param sha256Hash SHA256 hash 字符串
     * @param extension 文件扩展名（可选）
     * @param expiresInSeconds URL有效期（秒）
     * @param isDownload 是否作为附件下载（true）或内联显示（false）
     * @param filename 下载时的文件名（仅在isDownload=true时使用）
     * @param shortUrl 文件的短链接ID（用于缓存key）
     * @return 预签名URL
     */
    fun getDownloadUrl(
        sha256Hash: String, extension: String? = null, expiresInSeconds: Long = 604800, isDownload: Boolean = false,
        filename: String? = null, shortUrl: Long
    ): String
    {


        // 如果提供了shortUrl，尝试从Redis获取缓存的URL
        val cacheKey = "presigned_url:$shortUrl:${if (isDownload) "download" else "view"}"
        val cachedUrl = redisTemplate.opsForValue().get(cacheKey)
        if (!cachedUrl.isNullOrBlank())
        {
            log.info { "从缓存获取预签名URL: shortUrl=$shortUrl" }
            return cachedUrl
        }

        // 缓存未命中，生成新的预签名URL
        val newUrl= if (shortUrl < 100)
        {

           generatePresignedUrl(sha256Hash, extension, 2592000, isDownload, filename)

        } else
        {
            generatePresignedUrl(sha256Hash, extension, expiresInSeconds, isDownload, filename)
        }
        if(shortUrl <100)
        {
            redisTemplate.opsForValue().set(cacheKey, newUrl, java.time.Duration.ofSeconds(2582000))
        }else
        {
            redisTemplate.opsForValue().set(cacheKey, newUrl, java.time.Duration.ofSeconds(expiresInSeconds))
        }
        // 缓存7天（604800秒）
        log.info { "生成并缓存预签名URL: shortUrl=$shortUrl, 有效期=7天" }
        return newUrl
    }

    /**
     * 实际生成预签名URL的内部方法
     */
    private fun generatePresignedUrl(
        sha256Hash: String, extension: String?, expiresInSeconds: Long, isDownload: Boolean, filename: String?
    ): String
    {
        val objectKey = buildObjectKey(sha256Hash, extension)
        val input = PreSignedURLInput().setBucket(bucketName).setKey(objectKey).setHttpMethod(HttpMethod.GET)
            .setExpires(expiresInSeconds)

        // 设置Content-Disposition响应头
        if (isDownload && !filename.isNullOrBlank())
        {
            val map = mapOf(
                "response-content-disposition" to "attachment; filename=\"$filename\"",
                "Content-Type" to "application/octet-stream"
            )
            input.header = map
        }

        val output = tos.preSignedURL(input)
        return output.signedUrl
    }

    /**
     * 生成预签名POST表单数据，用于前端直传
     * @param sha256Hash SHA256 hash 字符串
     * @param extension 文件扩展名
     * @param expiresInSeconds 签名有效期（秒）
     * @return 包含policy、signature等字段的Map
     */
    fun generatePresignedPostSignature(
        sha256Hash: String, extension: String, expiresInSeconds: Long = 6
    ): Map<String, String>
    {
        val objectKey = buildObjectKey(sha256Hash, extension)
        val input = PreSignedPostSignatureInput().setBucket(bucketName).setKey(objectKey).setExpires(expiresInSeconds)

        val output = tos.preSignedPostSignature(input)

        return mapOf(
            "url" to "https://${bucketName}.tos-cn-beijing.volces.com", "key" to objectKey, "policy" to output.policy,
            "algorithm" to output.algorithm, "credential" to output.credential, "date" to output.date,
            "signature" to output.signature
        )
    }

    fun delete(sha256Hash: String, extension: String? = null): DeleteObjectOutput
    {
        val objectKey = buildObjectKey(sha256Hash, extension)
        val input = DeleteObjectInput().setBucket(bucketName).setKey(objectKey)
        return tos.deleteObject(input)
    }

    @Throws(IOException::class)
    fun calculateFileSHA256(file: MultipartFile): String
    {
        file.inputStream.use { `is` ->
            return DigestUtils.sha256Hex(`is`)
        }
    }


    /**
     * @deprecated 已废弃，建议使用getDownloadUrl生成预签名URL后重定向，减轻服务器压力
     * 直接通过服务器流式传输文件
     */
    @Deprecated("使用getDownloadUrl替代", ReplaceWith("getDownloadUrl(objectKey, 300, isDownload, name)"))
    fun getFile(objectKey: String, isDownload: Boolean, response: HttpServletResponse, name: String)
    {
        if (!isFileExistInCos(objectKey))
        {
            throw BSystemException("文件不存在：$objectKey")
        }
        this.setResponseHeader(response, objectKey, isDownload, name)
        val obj = GetObjectV2Input().setBucket(bucketName).setKey(objectKey)
        writeStream(obj, response)
    }

    private fun writeStream(obj: GetObjectV2Input, response: HttpServletResponse)
    {
        tos.getObject(obj).use { output: GetObjectV2Output ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            val outputStream = response.outputStream
            while ((output.content.read(buffer).also { bytesRead = it }) != -1)
            {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
        }
    }

    /**
     * @deprecated 已废弃，建议使用getDownloadUrl生成预签名URL后重定向
     * 带限速的文件流式传输（limit单位是MB）
     */
    @Deprecated("使用getDownloadUrl替代", ReplaceWith("getDownloadUrl(objectKey, 300, isDownload, name)"))
    fun getFileRateLimit(
        objectKey: String, isDownload: Boolean, response: HttpServletResponse, limit: Long, name: String
    )
    {
        if (!isFileExistInCos(objectKey))
        {
            throw BSystemException("文件不存在：$objectKey")
        }
        val trafficLimit = (limit * 8 * 1024 * 1024)
        val options = ObjectMetaRequestOptions()
        options.trafficLimit = trafficLimit
        this.setResponseHeader(response, objectKey, isDownload, name)
        val obj = GetObjectV2Input().setBucket(bucketName).setKey(objectKey).setOptions(options)
        writeStream(obj, response)
    }

    private fun getContentTypeByFilename(filename: String): String
    {
        val suffix = filename.lowercase().substringAfterLast(".", "")
        return when (suffix)
        {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "bmp" -> "image/bmp"
            "txt" -> "text/plain; charset=utf-8"
            "md" -> "text/markdown; charset=utf-8"
            "pdf" -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            else -> "application/octet-stream"
        }
    }

    @Throws(IOException::class)
    private fun setResponseHeader(response: HttpServletResponse, objectKey: String, isDownload: Boolean, name: String)
    {
        val contentType = getContentTypeByFilename(objectKey)
        response.contentType = contentType
        if (isDownload)
        {
            // 下载模式：禁用缓存
            val input = HeadObjectV2Input().setBucket(bucketName).setKey(objectKey)
            val output = tos.headObject(input)
            val fileSize = output.contentLength.toInt() // 直接获取文件字节数
            if (fileSize > 0) response.setContentLength(fileSize)
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate")
            response.setHeader("Pragma", "no-cache")
            response.setHeader("Expires", "0")
        }
        response.setHeader("Cache-Control", "public, max-age=3600")
        if (isDownload)
        {
            response.setHeader("Content-Disposition", "attachment; filename=\"$name\"")
        } else
        {
            response.setHeader("Content-Disposition", "inline; filename=\"$name\"")
        }
    }


    private fun isFileExistInCos(objectKey: String): Boolean
    {
        // 入参校验（避免空key导致无效请求）
        if (objectKey.isBlank())
        {
            log.warn { "校验TOS文件存在性失败：key为空" }
            return false
        }
        try
        {
            val input = HeadObjectV2Input().setBucket(bucketName).setKey(objectKey)
            tos.headObject(input)
            return true
        } catch (e: TosServerException)
        {
            if (e.statusCode == 404)
            {
                return false
            } else
            {
                log.error { e }
            }
        } catch (e: TosClientException)
        {
            log.error { "Client error: " + e.message }
        }
        return false
    }
}


