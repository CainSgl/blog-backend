package com.cainsgl.article.service

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.service.IService
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.cainsgl.api.ai.AiService
import com.cainsgl.api.article.post.chunk.PostChunkVectorService
import com.cainsgl.article.dto.ChunkScore
import com.cainsgl.article.dto.PostChunkScoreResult
import com.cainsgl.article.repository.PostChunkVectorMapper
import com.cainsgl.article.util.GfmChunkUtils
import com.cainsgl.common.entity.article.PostChunkVectorEntity
import com.cainsgl.common.entity.article.PostEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import kotlin.math.ln

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
        
        // 从数据库查询完整的文章信息（包含content）
        val query = KtQueryWrapper(PostEntity::class.java)
            .select(PostEntity::id, PostEntity::content)
            .eq(PostEntity::id, postId)
        val post = postService.getOne(query) ?: return false
        
        val content = post.content
        if (content.isNullOrEmpty()) {
            logger.warn { "文章${postId}内容为空，跳过重新向量化" }
            return false
        }
        
        if (originContent == content)
        {
            //内容无变更，不需要管
            return true
        }
        
        val postIdValue = post.id ?: run {
            logger.error { "文章ID为空，跳过重新向量化" }
            return false
        }
        
        //获取原先所有的chunk
        val wrapper = KtQueryWrapper(PostChunkVectorEntity::class.java).eq(PostChunkVectorEntity::postId, postIdValue)
        val list: List<PostChunkVectorEntity> = list(wrapper)
        //下面本质上是优化，他的目的其实是删除所有原来的向量化的结果，然后再重新向量化，但是向量化成本略高，所以就有下面的优化手段
        //切割原文进行差量分析，这里提供两种切割方式，第一个是按markdown切分成块，第二个是靠ai，这里目前使用第一种方案，第二种是在文章很水，口水话的时候比较推荐
        val chunks: List<String> = GfmChunkUtils.chunk(content)
        if (chunks.isEmpty()) {
            logger.warn { "文章${postId}切分后无内容块，跳过重新向量化" }
            return false
        }
        
        //进行hash运算，hash相同的过滤掉，我们只看hash不同的
        val existingMap = list.associateBy { it.calculateHash().hash!! }.toMutableMap()
        val addedChunks = mutableListOf<String>()
        for (chunk in chunks)
        {
            val hash = DigestUtils.sha256Hex(chunk)
            if (existingMap.remove(hash) == null)
            {
                //说明现在的文档了多了这一块
                addedChunks.add(chunk)
            }
        }
        //existingMap里面现在的值就是需要移除的了，因为现在切分后的块是没有里面的内容的
        val willRemove = existingMap.values.toList()
        val willInsert = mutableListOf<PostChunkVectorEntity>()
        
        if (addedChunks.isEmpty()) {
            // 只有删除操作，没有新增
            logger.debug { "重新加载向量的文档${postIdValue}, 无新增块，删除${willRemove.size}个块" }
            return transactionTemplate.execute { status ->
                if (willRemove.isNotEmpty())
                {
                    removeByIds(willRemove)
                }
                return@execute true
            } ?: false
        }
        
        //去向量化所有
        val embeddings = aiService.getEmbedding(addedChunks) ?: run {
            logger.error { "获取文章${postId}的向量失败" }
            return false
        }
        
        embeddings.forEachIndexed { i, it ->
            if (it != null) {
                willInsert.add(
                    PostChunkVectorEntity(
                        postId = postIdValue,
                        chunk = addedChunks[i],
                        vector = it,
                    ).calculateHash()
                )
            }
        }
        logger.debug { "重新加载向量的文档${postIdValue}, 新增${addedChunks.size}个块，删除${willRemove.size}个块" }
        //开启事务
        return transactionTemplate.execute { status ->
            if (willInsert.isNotEmpty())
            {
                saveBatch(willInsert)
            }
            if (willRemove.isNotEmpty())
            {
                removeByIds(willRemove)
            }
            return@execute true
        } ?: false
    }

    fun loadVector(postId: Long): Boolean
    {

        val query = KtQueryWrapper(PostEntity::class.java)
            .select(PostEntity::id, PostEntity::content)
            .eq(PostEntity::id, postId)
        val post = postService.getOne(query) ?: return false
        
        return loadVector(post)
    }


     fun loadVector(post: PostEntity): Boolean
    {
        val content = post.content
        if (content.isNullOrEmpty()) {
            logger.warn { "文章${post.id}内容为空，跳过向量化" }
            return false
        }
        
        val postIdValue = post.id ?: run {
            logger.error { "文章ID为空，跳过向量化" }
            return false
        }
        
        val chunks: List<String> = GfmChunkUtils.chunk(content)
        if (chunks.isEmpty()) {
            logger.warn { "文章${postIdValue}切分后无内容块，跳过向量化" }
            return false
        }
        
        val embeddings = aiService.getEmbedding(chunks) ?: run {
            logger.error { "获取文章${postIdValue}的向量失败" }
            return false
        }
        
        if (embeddings.isEmpty()) {
            logger.warn { "文章${postIdValue}向量化结果为空" }
            return false
        }
        
        val willInsert = mutableListOf<PostChunkVectorEntity>()
        embeddings.forEachIndexed { i, it ->
            if (it != null) {
                willInsert.add(
                    PostChunkVectorEntity(
                        postId = postIdValue,
                        chunk = chunks[i],
                        vector = it,
                    ).calculateHash()
                )
            }
        }
        
        if (willInsert.isEmpty()) {
            logger.warn { "文章${postIdValue}没有有效的向量数据" }
            return false
        }
        
        val wrapper = KtQueryWrapper(PostChunkVectorEntity::class.java).eq(PostChunkVectorEntity::postId, postIdValue)
        remove(wrapper)
        getBaseMapper().insert(willInsert)
        return true
    }

    override fun removeVector(postId: Long): Boolean
    {
        val wrapper =KtQueryWrapper(PostChunkVectorEntity::class.java).eq(PostChunkVectorEntity::postId, postId)
        return remove(wrapper)
    }

    /**
     * 通过向量搜索获取相关文章（加权聚合算法）
     * 聚合得分 = minDistance / (1 + ln(1 + hitCount))
     * 命中chunk越多、距离越小，得分越低（越相似）
     */
    fun getPostsByVector(targetVector: FloatArray, startValue: Double, limit: Int = 10): List<PostChunkScoreResult>
    {
        // 多取一些记录，因为同一篇文章可能有多个块
        val chunkResults = baseMapper.selectPostsByCosine(targetVector, startValue, limit * 5)
        // 按 postId 分组
        val grouped = chunkResults.groupBy { it.postId }

        // 批量获取文章信息（排除大字段）
        val postIds = grouped.keys.toList()
        val postMapper = postService.baseMapper
        val postInfoMap = if (postIds.isNotEmpty())
        {
            postMapper.selectBasicInfoByIds(postIds).associateBy { it.id }
        } else
        {
            emptyMap()
        }

        return grouped.map { (postId, results) ->
            val minDistance = results.minOf { it.distance }
            val hitCount = results.size
            // 加权聚合：距离越小越好，命中数越多越好
            val aggregatedScore = minDistance / (1 + ln(1.0 + hitCount))

            val chunks = results
                .sortedBy { it.distance }
                .map { ChunkScore(chunk = it.chunk ?: "", score = it.distance) }
            val article = postInfoMap[postId] ?: PostEntity(title = "Fail：获取文章标题失败")
            PostChunkScoreResult(
                article = article,
                aggregatedScore = aggregatedScore,
                chunks = chunks
            )
        }
            .sortedBy { it.aggregatedScore }
            .take(limit)
    }


}
