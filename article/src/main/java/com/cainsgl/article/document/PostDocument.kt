package com.cainsgl.article.document

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.Setting

/**
 * 文章 ES 文档
 * 用于全文搜索和评分排序
 */
@Document(indexName = "posts")
@Setting(shards = 3, replicas = 1)
data class PostDocument(
    @Id
    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long,

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    var title: String,

    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_smart")
    var content: String?,

    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_smart", store = false)
    var summary: String?,
    @Field(type = FieldType.Keyword)
    var img: String?,
    @Field(type = FieldType.Keyword)
    var tags: List<String>?,
    @Field(type = FieldType.Double)
    var score: Double = 0.0
) {
    companion object {
        /**
         * 计算文章综合得分
         */
        fun calculateScore(likeCount: Int, viewCount: Int, commentCount: Int): Double {
            return (likeCount * 2.0 + viewCount * 0.5 + commentCount * 1.5)
        }
    }
}
