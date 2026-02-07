package com.cainsgl.file.controller

import cn.dev33.satoken.annotation.SaCheckRole
import com.volcengine.tos.TOSV2
import com.volcengine.tos.TosClientException
import com.volcengine.tos.TosServerException
import com.volcengine.tos.model.`object`.*
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * Brotli 压缩文件扫描和处理 Controller
 * 该 Controller 完全独立，不依赖数据库，仅使用 TOS API 进行操作
 */
@RestController
@RequestMapping("/brotli")
class BrotliScanController
{
    @Resource(name = "frontOssClient")
    lateinit var frontOssClient: TOSV2


     val bucketName: String="front"

    /**
     * 扫描并处理所有 .br 文件
     * 1. 扫描桶中所有 .br 结尾的文件
     * 2. 复制为去掉 .br 后缀的新文件
     * 3. 设置正确的 Content-Type 和 Content-Encoding
     * 4. 删除原 .br 文件
     *
     * @param prefix 可选的前缀过滤，只处理指定前缀下的文件
     * @param dryRun 是否为试运行模式（true=只扫描不处理，false=实际处理）
     * @return 处理结果统计
     */
    @SaCheckRole("admin")
    @PostMapping("/scan-and-process")
    fun scanAndProcessBrotliFiles(
        @RequestParam(required = false) prefix: String?,
        @RequestParam(defaultValue = "false") dryRun: Boolean
    ): Map<String, Any>
    {
        log.info { "开始扫描 Brotli 文件，bucket=$bucketName, prefix=$prefix, dryRun=$dryRun" }

        val results = mutableListOf<ProcessResult>()
        var continuationToken: String? = null
        var totalScanned = 0
        var totalProcessed = 0
        var totalFailed = 0

        try
        {
            do
            {
                // 构建列举请求
                val listInput = ListObjectsType2Input()
                    .setBucket(bucketName)
                    .setMaxKeys(1000)
                    .apply {
                        if (!prefix.isNullOrBlank()) setPrefix(prefix)
                        if (!continuationToken.isNullOrBlank()) setContinuationToken(continuationToken)
                    }

                val listOutput = frontOssClient.listObjectsType2(listInput)
                totalScanned += listOutput.contents?.size ?: 0

                // 处理每个对象
                listOutput.contents?.forEach { obj ->
                    if (obj.key.endsWith(".br"))
                    {
                        val result = if (dryRun)
                        {
                            // 试运行模式：只记录，不实际处理
                            ProcessResult(
                                originalKey = obj.key,
                                newKey = obj.key.removeSuffix(".br"),
                                status = "DRY_RUN",
                                message = "试运行模式，未实际处理"
                            )
                        } else
                        {
                            // 实际处理文件
                            processBrotliFile(obj.key)
                        }

                        results.add(result)
                        if (result.status == "SUCCESS") totalProcessed++
                        else if (result.status == "FAILED") totalFailed++
                    }
                }

                continuationToken = if (listOutput.isTruncated) listOutput.nextContinuationToken else null

            } while (!continuationToken.isNullOrBlank())

            log.info { "扫描完成，总计扫描=$totalScanned, 处理成功=$totalProcessed, 失败=$totalFailed" }

            return mapOf(
                "success" to true,
                "dryRun" to dryRun,
                "totalScanned" to totalScanned,
                "totalBrotliFiles" to results.size,
                "totalProcessed" to totalProcessed,
                "totalFailed" to totalFailed,
                "details" to results
            )

        } catch (e: TosServerException)
        {
            log.error(e) { "TOS 服务端错误: StatusCode=${e.statusCode}, Code=${e.code}, Message=${e.message}" }
            return mapOf(
                "success" to false,
                "error" to "TOS 服务端错误: ${e.message}",
                "statusCode" to e.statusCode,
                "totalScanned" to totalScanned,
                "totalProcessed" to totalProcessed,
                "totalFailed" to totalFailed
            )
        } catch (e: TosClientException)
        {
            log.error(e) { "TOS 客户端错误: ${e.message}" }
            return mapOf(
                "success" to false,
                "error" to "TOS 客户端错误: ${e.message}",
                "totalScanned" to totalScanned,
                "totalProcessed" to totalProcessed,
                "totalFailed" to totalFailed
            )
        } catch (e: Exception)
        {
            log.error(e) { "未知错误: ${e.message}" }
            return mapOf(
                "success" to false,
                "error" to "未知错误: ${e.message}",
                "totalScanned" to totalScanned,
                "totalProcessed" to totalProcessed,
                "totalFailed" to totalFailed
            )
        }
    }

    /**
     * 处理单个 Brotli 文件
     * 1. 检查原文件是否存在
     * 2. 复制为新文件名（去掉 .br 后缀）
     * 3. 设置元数据（Content-Type 和 Content-Encoding）
     * 4. 删除原 .br 文件
     */
    private fun processBrotliFile(originalKey: String): ProcessResult
    {
        val newKey = originalKey.removeSuffix(".br")
        log.info { "处理文件: $originalKey -> $newKey" }

        try
        {
            // 1. 检查原文件是否存在
            if (!checkObjectExists(originalKey))
            {
                return ProcessResult(
                    originalKey = originalKey,
                    newKey = newKey,
                    status = "FAILED",
                    message = "原文件不存在"
                )
            }

            // 2. 复制对象（重命名）
            val copyInput = CopyObjectV2Input()
                .setBucket(bucketName)
                .setKey(newKey)
                .setSrcBucket(bucketName)
                .setSrcKey(originalKey)

            frontOssClient.copyObject(copyInput)
            log.info { "复制成功: $originalKey -> $newKey" }

            // 3. 设置元数据
            val contentType = getContentTypeByFilename(newKey)
            val options = ObjectMetaRequestOptions()
                .setContentType(contentType)
                .setContentEncoding("br")

            val setMetaInput = SetObjectMetaInput()
                .setBucket(bucketName)
                .setKey(newKey)
                .setOptions(options)

            frontOssClient.setObjectMeta(setMetaInput)
            log.info { "设置元数据成功: $newKey, Content-Type=$contentType, Content-Encoding=br" }

            // 4. 删除原 .br 文件
            val deleteInput = DeleteObjectInput()
                .setBucket(bucketName)
                .setKey(originalKey)

            frontOssClient.deleteObject(deleteInput)
            log.info { "删除原文件成功: $originalKey" }

            return ProcessResult(
                originalKey = originalKey,
                newKey = newKey,
                status = "SUCCESS",
                message = "处理成功，Content-Type=$contentType"
            )

        } catch (e: TosServerException)
        {
            log.error(e) { "处理文件失败: $originalKey, StatusCode=${e.statusCode}, Message=${e.message}" }
            return ProcessResult(
                originalKey = originalKey,
                newKey = newKey,
                status = "FAILED",
                message = "TOS 服务端错误: ${e.message}"
            )
        } catch (e: TosClientException)
        {
            log.error(e) { "处理文件失败: $originalKey, Message=${e.message}" }
            return ProcessResult(
                originalKey = originalKey,
                newKey = newKey,
                status = "FAILED",
                message = "TOS 客户端错误: ${e.message}"
            )
        } catch (e: Exception)
        {
            log.error(e) { "处理文件失败: $originalKey, Message=${e.message}" }
            return ProcessResult(
                originalKey = originalKey,
                newKey = newKey,
                status = "FAILED",
                message = "未知错误: ${e.message}"
            )
        }
    }

    /**
     * 检查对象是否存在
     */
    private fun checkObjectExists(objectKey: String): Boolean
    {
        return try
        {
            val input = HeadObjectV2Input()
                .setBucket(bucketName)
                .setKey(objectKey)
            frontOssClient.headObject(input)
            true
        } catch (e: TosServerException)
        {
            if (e.statusCode == 404)
            {
                false
            } else
            {
                throw e
            }
        }
    }

    /**
     * 根据文件名获取 Content-Type
     */
    private fun getContentTypeByFilename(filename: String): String
    {
        val suffix = filename.lowercase().substringAfterLast(".", "")
        return when (suffix)
        {
            // JavaScript
            "js" -> "application/javascript"
            "mjs" -> "application/javascript"
            
            // CSS
            "css" -> "text/css"
            
            // HTML
            "html", "htm" -> "text/html"
            
            // JSON
            "json" -> "application/json"
            
            // XML
            "xml" -> "application/xml"
            
            // 图片
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "ico" -> "image/x-icon"
            
            // 字体
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "eot" -> "application/vnd.ms-fontobject"
            
            // 文本
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            
            // 其他
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            
            else -> "application/octet-stream"
        }
    }

    /**
     * 处理结果数据类
     */
    data class ProcessResult(
        val originalKey: String,
        val newKey: String,
        val status: String, // SUCCESS, FAILED, DRY_RUN
        val message: String
    )
}
