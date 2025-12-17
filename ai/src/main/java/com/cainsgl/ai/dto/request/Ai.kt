package com.cainsgl.ai.dto.request

import jakarta.validation.constraints.NotEmpty

//只有一个字段的必须有空构造器，我也不知道为什么
data class ContentRequest(
    @field:NotEmpty(message = "content不能为空")
    val content:String,
)
{
    constructor():this("")
}