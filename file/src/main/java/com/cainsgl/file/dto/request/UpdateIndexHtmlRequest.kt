package com.cainsgl.file.dto.request

import jakarta.validation.constraints.NotBlank

/**
 * 更新 index.html 内容请求
 */
data class UpdateIndexHtmlRequest(
    @field:NotBlank(message = "内容不能为空")
    val content: String
)
