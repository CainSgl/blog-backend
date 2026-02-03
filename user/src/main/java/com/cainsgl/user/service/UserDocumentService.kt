package com.cainsgl.user.service

import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import com.cainsgl.user.document.UserDocument
import jakarta.annotation.Resource
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder
import org.springframework.data.elasticsearch.core.query.HighlightQuery
import org.springframework.data.elasticsearch.core.query.highlight.Highlight
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters
import org.springframework.stereotype.Service

@Service
class UserDocumentService
{
    @Resource
    lateinit var elasticsearchOperations: ElasticsearchOperations

    /**
     * 保存单个用户文档
     */
    fun save(userDocument: UserDocument)
    {
        elasticsearchOperations.save(userDocument, IndexCoordinates.of("users"))
    }

    /**
     * 删除用户文档
     */
    fun delete(id: Long)
    {
        elasticsearchOperations.delete(id.toString(), IndexCoordinates.of("users"))
    }

    /**
     * 批量保存用户文档
     */
    fun saveAll(userDocuments: List<UserDocument>)
    {
        elasticsearchOperations.save(userDocuments, IndexCoordinates.of("users"))
    }

    /**
     * 根据昵称搜索用户
     * @param query 搜索关键词
     * @param size 每页大小
     * @param searchAfter 上一页最后一条的排序值（用于分页）
     * @return 搜索结果和下一页的 searchAfter 值
     */
    fun search(
        query: String,
        size: Int = 20,
        searchAfter: List<Any>?
    ): UserSearchResult
    {
        return retryOnConnectionReset(maxRetries = 3) {
            performSearch(query, size, searchAfter)
        }
    }

    private fun performSearch(
        query: String,
        size: Int,
        searchAfter: List<Any>?
    ): UserSearchResult
    {
        val boolQueryBuilder = BoolQuery.Builder()

        // 昵称匹配
        val shouldQueries = mutableListOf<Query>()
        shouldQueries.add(
            Query.Builder().match(
                MatchQuery.Builder()
                    .field("nickname")
                    .query(query)
                    .boost(1.0f)
                    .build()
            ).build()
        )

        boolQueryBuilder.should(shouldQueries)
        boolQueryBuilder.minimumShouldMatch("1")

        // 只返回 id 和 nickname 字段
        val sourceFilter = FetchSourceFilterBuilder()
            .withIncludes("id", "nickname")
            .build()

        // 高亮配置
        val highlight = Highlight(
            HighlightParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .build(),
            listOf(HighlightField("nickname"))
        )

        // 构建查询
        val nativeQueryBuilder = NativeQuery.builder()
            .withQuery { q -> q.bool(boolQueryBuilder.build()) }
            .withSourceFilter(sourceFilter)
            .withHighlightQuery(HighlightQuery(highlight, UserDocument::class.java))
            .withMaxResults(size)
            .withSort { s -> s.score { it.order(SortOrder.Desc) } }
            .withSort { s -> s.field { f -> f.field("id").order(SortOrder.Desc) } }

        // 如果有 searchAfter，添加分页参数
        searchAfter?.let {
            nativeQueryBuilder.withSearchAfter(it)
        }

        val searchHits = elasticsearchOperations.search(
            nativeQueryBuilder.build(),
            UserDocument::class.java
        )

        val documents = searchHits.searchHits.map { hit ->
            val doc = hit.content
            val highlightFields = hit.highlightFields
            highlightFields["nickname"]?.firstOrNull()?.let { doc.nickname = it }
            doc
        }

        val nextSearchAfter = searchHits.searchHits.lastOrNull()?.sortValues

        return UserSearchResult(
            data = documents,
            searchAfter = nextSearchAfter,
            total = searchHits.totalHits,
            hasMore = documents.size == size
        )
    }

    private fun <T> retryOnConnectionReset(maxRetries: Int = 3, delayMs: Long = 500, block: () -> T): T
    {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try
            {
                return block()
            } catch (e: org.springframework.dao.DataAccessResourceFailureException)
            {
                lastException = e
                if (e.message?.contains("Connection reset") == true && attempt < maxRetries - 1)
                {
                    Thread.sleep(delayMs * (attempt + 1))
                } else
                {
                    throw e
                }
            } catch (e: Exception)
            {
                throw e
            }
        }
        throw lastException ?: RuntimeException("Retry failed")
    }
}

data class UserSearchResult(
    val data: List<UserDocument>,
    val searchAfter: List<Any>?,
    val total: Long,
    val hasMore: Boolean
)
