package com.cainsgl.article.service

import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery
import com.cainsgl.article.document.PostDocument

import jakarta.annotation.Resource
import org.jsoup.Jsoup
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
class PostDocumentService
{

    @Resource
    lateinit var elasticsearchOperations: ElasticsearchOperations

    fun save(postDocument: PostDocument)
    {
        //对content去除标签
        postDocument.content = postDocument.content?.let { str -> Jsoup.parse(str).text() }
        //对title和summary去除标签
        postDocument.title = postDocument.title.let { str -> Jsoup.parse(str).text() }
        postDocument.summary = postDocument.summary?.let { str -> Jsoup.parse(str).text() }
        elasticsearchOperations.save(postDocument, IndexCoordinates.of("posts"))
    }

    fun delete(id: Long)
    {
        elasticsearchOperations.delete(id.toString(), IndexCoordinates.of("posts"))
    }

    fun saveAll(postDocuments: List<PostDocument>)
    {
        // 使用elasticsearchOperations进行批量保存
        postDocuments.forEach { it.content = it.content?.let { str -> Jsoup.parse(str).text() } }
        //去除title和summary中的标签
        postDocuments.forEach { it.title = it.title.let { str -> Jsoup.parse(str).text() } }
        postDocuments.forEach { it.summary = it.summary?.let { str -> Jsoup.parse(str).text() } }
        elasticsearchOperations.save(postDocuments, IndexCoordinates.of("posts"))
    }

    /**
     * 复合搜索查询
     * @param query 搜索关键词
     * @param useTag 是否匹配标签（true: 标签匹配也算，false: 不考虑标签）
     * @param useContent 是否匹配内容（true: 匹配 title+content+summary，false: 只匹配 title）
     * @param size 每页大小
     * @param searchAfter 上一页最后一条的排序值（用于分页）
     * @return 搜索结果和下一页的 searchAfter 值
     */
    fun search(
        query: String, useTag: Boolean, useContent: Boolean, size: Int = 20, searchAfter: List<Any>?
    ): SearchResult
    {
        val boolQueryBuilder = BoolQuery.Builder()
        // 构建 should 查询（至少匹配一个）
        val shouldQueries = mutableListOf<Query>()


        // 标题匹配（权重最高）
        shouldQueries.add(
            Query.Builder().match(
                    MatchQuery.Builder().field("title").query(query).boost(3.0f).build()
                ).build()
        )

        // 如果 useContent=true，添加 content 和 summary 匹配
        if (useContent)
        {
            shouldQueries.add(
                Query.Builder().match(
                        MatchQuery.Builder().field("content").query(query).boost(1.0f).build()
                    ).build()
            )
            shouldQueries.add(
                Query.Builder().match(
                        MatchQuery.Builder().field("summary").query(query).boost(2.0f).build()
                    ).build()
            )
        }

        // 如果 useTag=true，添加标签精确匹配
        if (useTag)
        {
            shouldQueries.add(
                Query.Builder().terms(
                        TermsQuery.Builder().field("tags").terms { t -> t.value(listOf(FieldValue.of(query))) }
                            .boost(2.5f).build()).build())
        }

        boolQueryBuilder.should(shouldQueries)
        boolQueryBuilder.minimumShouldMatch("1")
        val sourceFilter =
            FetchSourceFilterBuilder().withIncludes("id", "title", "img", "tags", "score", "summary").build()
        val highlight = Highlight(
            HighlightParameters.builder().withPreTags("<em>").withPostTags("</em>").build(), listOf(
                HighlightField("title"), HighlightField("summary"), HighlightField("tags")
            )
        )
        // 构建 NativeQuery
        val nativeQueryBuilder =
            NativeQuery.builder().withQuery { q -> q.bool(boolQueryBuilder.build()) }.withSourceFilter(sourceFilter)
                .withHighlightQuery(HighlightQuery(highlight, PostDocument::class.java)).withMaxResults(size)
                .withSort { s -> s.score { it.order(SortOrder.Desc) } }
                .withSort { s -> s.field { f -> f.field("id").order(SortOrder.Desc) } }

        // 如果有 searchAfter，添加分页参数
        searchAfter?.let {
            nativeQueryBuilder.withSearchAfter(it)
        }

        val searchHits = elasticsearchOperations.search(
            nativeQueryBuilder.build(), PostDocument::class.java
        )

        val documents = searchHits.searchHits.map { hit ->
            val doc = hit.content
            val highlightFields = hit.highlightFields
            highlightFields["title"]?.firstOrNull()?.let { doc.title = it }
            highlightFields["summary"]?.firstOrNull()?.let { doc.summary = it }
            doc
        }
        val nextSearchAfter = searchHits.searchHits.lastOrNull()?.sortValues

        return SearchResult(
            data = documents, searchAfter = nextSearchAfter, total = searchHits.totalHits,
            hasMore = documents.size == size
        )
    }
}

data class SearchResult(
    val data: List<PostDocument>, val searchAfter: List<Any>?, val total: Long, val hasMore: Boolean
)