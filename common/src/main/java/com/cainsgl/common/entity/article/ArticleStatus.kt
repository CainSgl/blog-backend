package com.cainsgl.common.entity.article

import com.baomidou.mybatisplus.annotation.EnumValue
import com.fasterxml.jackson.annotation.JsonValue

enum class ArticleStatus(
    @EnumValue // MyBatis-Plus 映射数据库枚举值
    val dbValue: String,
    @JsonValue // 前端展示描述
    val desc: String
)
{
    DRAFT("draft", "草稿"),
    PENDING_REVIEW("pending_review", "待审核"),
    PUBLISHED("published", "已发布"),
    OFF_SHELF("off_shelf", "已下架");

    companion object
    {
        fun fromDbValue(dbValue: String): ArticleStatus=
            entries.firstOrNull { it.dbValue == dbValue } ?: DRAFT
    }
}
