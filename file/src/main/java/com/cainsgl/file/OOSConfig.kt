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
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException


@Configuration
class OOSConfig(@Value("\${oos.secretId}") var accessKey: String, @Value("\${oos.secretKey}") var secretKey: String)
{
    @Bean
    fun oosClient(): TOSV2
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
    @Resource
    lateinit var tos: TOSV2

    @Value("\${oos.bucketName}")
    lateinit var bucketName: String

    @Throws(IOException::class)
    fun upload(file: MultipartFile): String
    {
        if (file.isEmpty)
        {
            throw BSystemException("上传的文件不能为空")
        }
        val originalFilename = file.originalFilename!!
        val fileSha256: String = "file/${calculateFileSHA256(file)}"
        var fileSuffix = ""
        if (originalFilename.contains("."))
        {
            fileSuffix = originalFilename.substring(originalFilename.lastIndexOf("."))
        }
        val objectKey: String = bucketName + fileSha256 + fileSuffix
        if (isFileExistInCos(objectKey))
        {
            // 已存在，直接返回访问URL，无需重复上传
            return objectKey
        }
        file.inputStream.use { stream ->
            val putObjectInput =
                PutObjectInput().setBucket(bucketName).setKey(objectKey).setContent(stream).setContentLength(file.size);
            val output: PutObjectOutput = tos.putObject(putObjectInput)
            log.info { "putObject succeed, object's etag is " + output.etag };
            log.info { "putObject succeed, object's crc64 is " + output.hashCrc64ecma };
            return objectKey
        }
    }

    fun delete(objectKey: String): DeleteObjectOutput
    {
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
     * 生成预签名下载URL
     * @param objectKey 对象存储的key
     * @param expiresInSeconds URL有效期（秒）
     * @param isDownload 是否作为附件下载（true）或内联显示（false）
     * @param filename 下载时的文件名（仅在isDownload=true时使用）
     * @return 预签名URL
     */
    fun getDownloadUrl(
        objectKey: String, expiresInSeconds: Long = 300, isDownload: Boolean = false, filename: String? = null
    ): String
    {
        val input = PreSignedURLInput().setBucket(bucketName).setKey(objectKey).setHttpMethod(HttpMethod.GET)
            .setExpires(expiresInSeconds)

        // 设置Content-Disposition响应头
        if (isDownload && !filename.isNullOrBlank())
        {
            val map = mapOf("response-content-disposition" to "attachment; filename=\"$filename\"",
                "Content-Type" to "application/octet-stream"
            )
            input.header = map
        }

        val output = tos.preSignedURL(input)
        return output.signedUrl
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
        options.setTrafficLimit(trafficLimit)
        this.setResponseHeader(response, objectKey, isDownload, name)
        val obj = GetObjectV2Input().setBucket(bucketName).setKey(objectKey).setOptions(options);
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
        response.setHeader("Cache-Control", "public, max-age=3600");
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
            val output = tos.headObject(input)
            return true
        } catch (e: TosServerException)
        {
            if (e.getStatusCode() == 404)
            {
                return false
            } else
            {
                log.error { e }
            }
        } catch (e: TosClientException)
        {
            log.error { "Client error: " + e.message };
        }
        return false
    }
}


