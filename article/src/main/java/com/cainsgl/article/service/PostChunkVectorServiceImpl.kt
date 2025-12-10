package com.cainsgl.article.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.ai.AiService
import com.cainsgl.api.article.post.chunk.PostChunkVectorService
import com.cainsgl.article.repository.PostChunkVectorMapper
import com.cainsgl.article.util.GfmChunkUtils
import com.cainsgl.common.entity.article.PostChunkVectorEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
private val logger = KotlinLogging.logger {}

@Service
class PostChunkVectorServiceImpl : ServiceImpl<PostChunkVectorMapper, PostChunkVectorEntity>(), PostChunkVectorService, IService<PostChunkVectorEntity>
{


    @Resource
    lateinit var aiService: AiService

    @Resource
    lateinit var postService: PostServiceImpl

    @Resource
    lateinit var transactionTemplate: TransactionTemplate
    /**
     * 获取向量信息
     * @param id
     * @return
     */
    fun getPostChunkVector(id: Long): PostChunkVectorEntity?
    {
        return baseMapper.selectById(id)
    }
    override fun reloadVector(postId: Long, originContent: String?): Boolean
    {
        if (originContent == null)
        {
            //说明之前没有向量化过，直接向量化
            loadVector(postId)
            return true
        }
        val post = postService.getPost(postId) ?: return false
        if (originContent == post.content)
        {
            //内容无变更，不需要管
            return true
        }
        //获取原先所有的chunk
        val wrapper = QueryWrapper<PostChunkVectorEntity>().eq("post_id", post.id)
        val list: List<PostChunkVectorEntity> = list(wrapper)
        //下面本质上是优化，他的目的其实是删除所有原来的向量化的结果，然后再重新向量化，但是向量化成本略高，所以就有下面的优化手段
        //切割原文进行差量分析，这里提供两种切割方式，第一个是按markdown切分成块，第二个是靠ai，这里目前使用第一种方案，第二种是在文章很水，口水话的时候比较推荐
        val chunks: List<String> = GfmChunkUtils.chunk(post.content!!)
        //进行hash运算，hash相同的过滤掉，我们只看hash不同的
        val existingMap = list.associateBy { it.calculateHash().hash!! }.toMutableMap()
        val addedChunks = mutableListOf<String>()
        for (chunk in chunks)
        {
            val hash = DigestUtils.sha256Hex(chunk)
            if (existingMap.remove(hash)==null)
            {
                //说明现在的文档了多了这一块
                addedChunks.add(chunk)
            }
        }
        //existingMap里面现在的值就是需要移除的了，因为现在切分后的块是没有里面的内容的
        val willRemove = existingMap.values.toList()
        val willInsert=mutableListOf<PostChunkVectorEntity>()
        //去向量化所有
        val embeddings = aiService.getEmbedding(addedChunks)
        embeddings.forEachIndexed { i, it ->
            willInsert.add(
                PostChunkVectorEntity(
                    postId = post.id,
                    chunk = addedChunks[i],
                    vector = it,
                ).calculateHash()
            )
        }
        logger.debug { "重新加载向量的文档${post.id}, 新增${addedChunks.size}个块，删除${willRemove.size}个块" }
        //开启事务
        return transactionTemplate.execute { status->
            if (willInsert.isNotEmpty()) {
              saveBatch(willInsert)
            }
            if (willRemove.isNotEmpty())
            {
               removeByIds(willRemove)
            }
            return@execute true
        }?:false
    }
    fun loadVector(postId: Long): Boolean
    {
        val post = postService.getPost(postId) ?: return false
        val chunks: List<String> = GfmChunkUtils.chunk(post.content!!)
        val embeddings = aiService.getEmbedding(chunks)
        val willInsert=mutableListOf<PostChunkVectorEntity>()
        embeddings.forEachIndexed { i, it ->
            willInsert.add(
                PostChunkVectorEntity(
                    postId = post.id,
                    chunk = chunks[i],
                    vector = it,
                ).calculateHash()
            )
        }
        val wrapper = QueryWrapper<PostChunkVectorEntity>().eq("post_id", post.id)
        remove(wrapper)
        getBaseMapper().insert(willInsert)
        return true
    }

    override fun removeVector(postId: Long): Boolean
    {
        val wrapper = QueryWrapper<PostChunkVectorEntity>().eq("post_id", postId)
        return remove(wrapper)
    }

}
