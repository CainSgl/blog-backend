package com.cainsgl.api.article.post

import com.cainsgl.common.entity.article.PostEntity

interface PostService {
    fun getById(id:Long) : PostEntity?
}
