package com.cainsgl.api.go

import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.PostEntity
import com.cainsgl.common.exception.BSystemException
import com.cainsgl.grpc.go.postClone.PostCloneServiceGrpc
import io.github.oshai.kotlinlogging.KotlinLogging
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}
@Component
class PostCloneGrpcService
{

    @GrpcClient("PostCloneService")
    lateinit var postCloneStub: PostCloneServiceGrpc.PostCloneServiceBlockingStub

    fun getPostById(id: Long): PostEntity?
    {
        require(id>0)
        val request = com.cainsgl.grpc.go.postClone.GetPostByIdRequest.newBuilder().setId(id).build()
        val postBasic = postCloneStub.getPostById(request)
        if (!postBasic.hasPost())
            return null
        val status = ArticleStatus.fromNumber(postBasic.post.status.number)
        val postEntity =
            PostEntity(id = postBasic.post.id, title = postBasic.post.title, content = postBasic.post.content, status = status)
        return postEntity
    }

    fun upsertPost(id:Long,title:String,content:String,status:ArticleStatus): Boolean
    {
        val request = com.cainsgl.grpc.go.postClone.UpsertPostRequest.newBuilder()
            .setPost(
                com.cainsgl.grpc.go.postClone.PostBasic.newBuilder()
                    .setId(id)
                    .setTitle(title)
                    .setContent(content)
                    .setStatus(com.cainsgl.grpc.go.postClone.ArticleStatus.forNumber(status.code))
                    .build()
            ).build()
        val upsertPostResponse = postCloneStub.upsertPost(request)
        if (!upsertPostResponse.hasPost())
            return false
        if (upsertPostResponse.post.status.number != status.code)
        {
            //状态不一致
            log.error { "发生了未知的错误，下游文章克隆grpc服务返回回来的状态与调用的不一致" }
            throw BSystemException("发生了未知的错误，下游文章克隆grpc服务返回回来的状态与调用的不一致")
        }
        return true
    }
    fun delete(id:Long): Boolean?
    {
        require(id>0)
        val request = com.cainsgl.grpc.go.postClone.DeletePostByIdRequest.newBuilder().setId(id).build()
        val deletePostByIdResponse = postCloneStub.deletePostById(request)
        return deletePostByIdResponse.success
    }
}