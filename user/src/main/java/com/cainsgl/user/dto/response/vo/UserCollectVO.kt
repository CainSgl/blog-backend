package com.cainsgl.user.dto.response.vo

import com.baomidou.mybatisplus.annotation.EnumValue
import com.cainsgl.common.entity.user.UserGroupEntity
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

enum class CollectType(
    @EnumValue
    val code: Int,
    @JsonValue
    val str: String
)
{
    POST(0, "文章"),
    KB(2, "知识库"),
    COMMENT(1, "评论"),
    UNKNOWN(-1, "未知");
    companion object
    {
        private val codeToEnumMap: Map<Int, CollectType> = CollectType.entries.associateBy { it.code }
        fun fromStr(str: String): CollectType =
            CollectType.entries.firstOrNull { it.str == str } ?: UNKNOWN

        fun fromNumber(number: Int): CollectType =
            codeToEnumMap[number] ?: UNKNOWN
    }
}

class UserCollectGroupVO(userCollect: UserGroupEntity)
{

    @JsonSerialize(using = ToStringSerializer::class)
    var id: Long? = null
    @JsonSerialize(using = ToStringSerializer::class)
    var name: String? = null
    @JsonSerialize(using = ToStringSerializer::class)
    var type: CollectType? = null
    init
    {
        this.id = userCollect.id
        this.name = userCollect.name
        this.type = CollectType.fromNumber(userCollect.type ?: -1)
    }
}