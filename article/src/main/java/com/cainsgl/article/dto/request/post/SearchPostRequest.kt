package com.cainsgl.article.dto.request.post

data class SearchPostRequest(
    var query: String?,
    var vectorOffset:Double?
)
{
}