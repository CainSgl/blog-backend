package com.cainsgl.article.dto

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

data class DirectoryTreeDTO(
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null,
    @JsonSerialize(using = ToStringSerializer::class)
    var kbId: Long? = null,
    @JsonSerialize(using = ToStringSerializer::class)
    var parentId: Long? = null,
    var name: String = "",
    @JsonSerialize(using = ToStringSerializer::class)
    var postId: Long? = null,
    var sortNum: Short = 0,
    var children: List<DirectoryTreeDTO> = emptyList()
)
