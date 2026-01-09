package com.cainsgl.api.article.post

import com.cainsgl.common.entity.article.PostEntity

interface PostService {
    fun getById(id:Long) : PostEntity?
    fun getVectorById(id:Long):FloatArray?
    fun addViewCount(id:Long,count:Int):Boolean
    fun addCommentCount(id:Long,count:Int):Boolean
}
