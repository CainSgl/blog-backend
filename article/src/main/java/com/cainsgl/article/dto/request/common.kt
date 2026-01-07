package com.cainsgl.article.dto.request

import com.cainsgl.common.entity.article.ArticleStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class PageUserIdListRequest(
    @field:Min(value = 1, message = "页码必须大于0")
    val page: Long = 1,
    @field:Min(value = 1, message = "每页数量必须大于0")
    @field:Max(value = 100, message = "每页数量不能超过100")
    val size: Long = 10,
    val userId: Long,
    val simple:Boolean=true,
    val keyword:String?=null,
    //下面是可选项，代表最新发布，最多播放，最多收藏
    val option:String?=null,
    //下面是只允许是当前用户才生效，方便用户看自己的文档哪些未发布
    var status: ArticleStatus?=null,
){
    companion object{
        val kbOptions:List<String> = listOf("created_at","like_count")
        val postOptions:List<String> =  listOf("published_at","view_count","like_count")
    }
}