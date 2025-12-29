package com.cainsgl.common.dto.response

data class PageResponse<T>(
    val records: List<T>,
    val total: Long,
    val pages: Long,
    val current: Long,
    val size: Long
)