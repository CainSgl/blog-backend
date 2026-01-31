package com.cainsgl.article.controller

import cn.dev33.satoken.stp.StpUtil
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.cainsgl.article.dto.request.CreateDirectoryRequest
import com.cainsgl.article.dto.request.MoveRequest
import com.cainsgl.article.dto.request.ReSortRequest
import com.cainsgl.article.dto.request.UpdateDirectoryRequest
import com.cainsgl.article.service.DirectoryServiceImpl
import com.cainsgl.article.service.KnowledgeBaseServiceImpl
import com.cainsgl.article.service.PostServiceImpl
import com.cainsgl.common.dto.response.ResultCode
import com.cainsgl.common.entity.article.ArticleStatus
import com.cainsgl.common.entity.article.KnowledgeBaseEntity
import com.cainsgl.common.entity.article.PostEntity
import jakarta.annotation.Resource
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.apache.rocketmq.client.core.RocketMQClientTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/post/dir")
class DirectoryController
{

    @Autowired
    private lateinit var rocketMQClientTemplate: RocketMQClientTemplate

    @Autowired
    private lateinit var postService: PostServiceImpl

    @Resource
    lateinit var directoryService: DirectoryServiceImpl
    @Resource
    lateinit var knowledgeBaseService: KnowledgeBaseServiceImpl
    @Resource
    lateinit var transactionTemplate: TransactionTemplate
    
    @PutMapping
    fun updateDirectory(@RequestBody @Valid request: UpdateDirectoryRequest): ResultCode
    {
        val userId = StpUtil.getLoginIdAsLong()
        if (directoryService.updateDirectory(request.id, request.kbId, userId, request.name, request.parentId))
        {
            return ResultCode.SUCCESS
        }
        //尝试推断是什么错误
        if (request.name != null && request.name.isEmpty())
        {
            return ResultCode.PARAM_INVALID
        }
        return ResultCode.RESOURCE_NOT_FOUND
    }

    
    @PostMapping("/resort")
    fun resort(@RequestBody @Valid request: ReSortRequest): ResultCode
    {
        // lastId允许为null（表示移到最前面）
        if (request.lastId != null && request.lastId < 0)
        {
            return ResultCode.PARAM_INVALID
        }
        val userId = StpUtil.getLoginIdAsLong()
        val resortDirectory = directoryService.resortDirectory(request.id, request.kbId, userId, request.lastId)
        directoryService.removeCache(request.kbId)
        return resortDirectory
    }

    
    @PostMapping("/move")
    fun resortAndUpdate(@RequestBody @Valid request: MoveRequest): ResultCode
    {
        // lastId允许为null（表示移到最前面）
        if (request.lastId != null && request.lastId < 0)
        {
            return ResultCode.PARAM_INVALID
        }
        val userId = StpUtil.getLoginIdAsLong()
        //先去更新目录到对应的父级目录下
       val res= transactionTemplate.execute{ status->
            //这里是有可能抛出运行时异常的
            if (!directoryService.updateDirectory(request.id, request.kbId, userId, null, request.parentId))
            {
                //没成功
                status.setRollbackOnly()
                return@execute ResultCode.DB_ERROR
            }
            val code = directoryService.resortDirectory(request.id, request.kbId, userId, request.lastId)
            if(code!=ResultCode.SUCCESS)
            {
                status.setRollbackOnly()
                return@execute code
            }
            return@execute code
        }

        if (res == null)
        {
            return ResultCode.UNKNOWN_ERROR
        }
        directoryService.removeCache(request.kbId)
        return res
    }


    @PostMapping
    fun createDirectory(@RequestBody @Valid request: CreateDirectoryRequest): Any
    {
        val userId = StpUtil.getLoginIdAsLong()
        val id = directoryService.saveDirectory(request.kbId, userId, request.name, request.parentId)
        if(id>0)
        {
            directoryService.removeCache(request.kbId)
            return id.toString()
        }
        return ResultCode.DB_ERROR
    }
    
    @DeleteMapping
    fun deleteDirectory(@RequestParam @Min(value = 1, message ="知识库id非法") kbId: Long,
                        @RequestParam @Min(value = 1,message = "目录id非法") dirId: Long): Int
    {
        val userId = StpUtil.getLoginIdAsLong()

        fun updatePost(ids:List<Long>):Boolean
        {
            if(ids.isEmpty())
            {
                return true
            }
            val updateWrapper= KtUpdateWrapper(PostEntity::class.java)
            updateWrapper.`in`(PostEntity::id,ids).eq(PostEntity::userId,userId).set(PostEntity::kbId,null).set(PostEntity::status,ArticleStatus.NO_KB)
            //获取所有的postId
            //设置知识库的数量扣减
            if(ids.isNotEmpty())
            {
                val updateWrapper2= KtUpdateWrapper(KnowledgeBaseEntity::class.java).eq(KnowledgeBaseEntity::id,kbId).setSql("post_count = post_count - ${ids.size}")
                knowledgeBaseService.update(updateWrapper2)
            }

           return postService.update(updateWrapper)
        }

        //这里返回的post的ids，后续发送mq的消息
        val res:List<Long>? = transactionTemplate.execute {
            //先去获取dir，如果这里的dir为null，要么不是该用户的，要么就是确实不存在
            val dir=directoryService.getDirectoryWithPermissionCheck(dirId, kbId, userId)?: return@execute emptyList<Long>()
            //需要删除对应的dir，在删除前先获取所有
            if(dir.postId!=null)
            {
                directoryService.removeById(dir.id)
                val lists = listOf(dir.postId!!)
                updatePost(lists)
                //说明是文档节点，不存在子节点，直接删除后返回
                return@execute lists
            }
            val dirLists= directoryService.getDirectoryAndSubdirectories(dirId) ?: return@execute emptyList<Long>()
            val postIds = dirLists.mapNotNull { it.postId }.distinct()
            directoryService.removeBatchByIds(dirLists)
            updatePost(postIds)
            return@execute postIds
        }
        if(res.isNullOrEmpty())
        {
            return 0
        }
        for(postId in res)
        {
            rocketMQClientTemplate.asyncSendNormalMessage("article:content", postId, null)
        }
        directoryService.removeCache(kbId)
        return res.size
    }
}
