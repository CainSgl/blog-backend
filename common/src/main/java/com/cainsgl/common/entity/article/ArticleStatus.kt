package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.EnumValue
import com.fasterxml.jackson.annotation.JsonValue

enum class ArticleStatus(
    @EnumValue // MyBatis-Plus 映射数据库枚举值
    val dbValue: String,
    @JsonValue // 前端展示描述
    val desc: String,
    val code: Int,
)
{
    UNSPECIFIED("UNSPECIFIED", "未指定",0),
    DRAFT("draft", "草稿",1),
    PENDING_REVIEW("pending_review", "待审核",2),
    PUBLISHED("published", "已发布",3),
    OFF_SHELF("off_shelf", "已下架",4),
    NO_KB("no_kb", "无知识库归属",5),
    ONLY_FANS("only_fans", "仅粉丝",6);

    companion object
    {
        private val codeToEnumMap: Map<Int, ArticleStatus> = entries.associateBy { it.code }
        fun fromDbValue(dbValue: String): ArticleStatus=
            entries.firstOrNull { it.dbValue == dbValue } ?: DRAFT
        fun fromNumber(number: Int): ArticleStatus=
            codeToEnumMap[number] ?: UNSPECIFIED
    }
}
