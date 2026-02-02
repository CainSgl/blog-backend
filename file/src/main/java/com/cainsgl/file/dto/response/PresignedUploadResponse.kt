package com.cainsgl.file.dto.response

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

/**
 * 预签名上传响应
 */
data class PresignedUploadResponse(
    // 上传的目标URL
    val url: String,
    
    // 对象存储的key
    val key: String,
    
    // 以下字段用于POST表单上传
    val policy: String,
    val algorithm: String,
    val credential: String,
    val date: String,
    val signature: String,
    
    // 文件记录ID（如果文件已存在）
    @field:JsonSerialize(using = ToStringSerializer::class)
    val fileId: Long? = null,
    
    // 是否需要上传（false表示文件已存在，无需上传）
    val needUpload: Boolean = true
)
