package com.cainsgl.api.article.post

import com.cainsgl.common.entity.article.PostEntity

interface PostService {
    fun getById(id:Long) : PostEntity?
    fun getByIds(ids: List<Long>): List<PostEntity>
    fun getVectorById(id:Long):FloatArray?
    fun addViewCount(id:Long,count:Int):Boolean
    @Deprecated("直接操作redis即可，无需grpc远程调用")
    fun addCommentCount(id:Long,count:Int):Boolean
}
