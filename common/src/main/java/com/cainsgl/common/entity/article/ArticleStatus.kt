package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.EnumValue
import com.fasterxml.jackson.annotation.JsonValue

enum class ArticleStatus(
    @EnumValue // MyBatis-Plus 映射数据库枚举值
    val dbValue: String,
    @JsonValue // 前端展示描述
    val desc: String,
    val code: Int,
    val public:Boolean,
)
{
    UNSPECIFIED("UNSPECIFIED", "未指定",0,false),
    DRAFT("draft", "草稿",1,false),
    PENDING_REVIEW("pending_review", "待审核",2,false),
    OFF_SHELF("off_shelf", "已下架",3,false),
    NO_KB("no_kb", "无知识库归属",4,false),
    PUBLISHED("published", "已发布",5,true),
    ONLY_FANS("only_fans", "仅粉丝",6,true);

    companion object
    {
        private val codeToEnumMap: Map<Int, ArticleStatus> = entries.associateBy { it.code }
        private val descToEnumMap: Map<String, ArticleStatus> = entries.associateBy { it.desc }
        fun fromDbValue(dbValue: String): ArticleStatus=
            entries.firstOrNull { it.dbValue == dbValue } ?: UNSPECIFIED
        fun fromNumber(number: Int): ArticleStatus=
            codeToEnumMap[number] ?: UNSPECIFIED
        fun fromDesc(desc: String):ArticleStatus
        {
            return descToEnumMap.getOrDefault(desc, UNSPECIFIED)
        }
    }

}
