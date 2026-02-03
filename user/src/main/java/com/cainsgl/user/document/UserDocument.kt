package com.cainsgl.user.document

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.Setting

/**
 * 用户 ES 文档
 * 用于昵称搜索
 */
@Document(indexName = "users")
@Setting(shards = 3, replicas = 1)
data class UserDocument(
    @Id
    @field:JsonSerialize(using = ToStringSerializer::class)
    var id: Long,

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    var nickname: String
)
