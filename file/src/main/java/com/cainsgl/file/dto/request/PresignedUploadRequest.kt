package com.cainsgl.file.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * 请求预签名上传凭证
 */
data class PresignedUploadRequest(
    @field:NotBlank(message = "文件名不能为空")
    val filename: String,
    
    @field:NotNull(message = "文件大小不能为空")
    val fileSize: Long,
    
    @field:NotBlank(message = "文件SHA256不能为空")
    val sha256: String
)
