package com.cainsgl.article.dto

data class DirectoryTreeDTO(
    var id: Long? = null,
    var kbId: Long? = null,
    var parentId: Long? = null,
    var name: String = "",
    var postId: Long? = null,
    var sortNum: Short = 0,
    var children: List<DirectoryTreeDTO>? = null
)
